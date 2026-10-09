/* Spike do redutor de ruido (ADR-015): processa um WAV PCM16 mono de 48 kHz com o RNNoise (quadros de 10 ms = 480 amostras) e
 * mede o tempo por quadro. Uso: rnnoise_wav entrada.wav saida.wav [intensidade 0..1] [--tempo]
 * A intensidade mistura seco/molhado POR AMOSTRA. A latencia do RNNoise v0.2 e de 2 quadros (960 amostras = 20 ms), medida
 * por correlacao no spike; o CLI ja devolve a saida alinhada a entrada. */
#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include "rnnoise.h"

enum { FRAME = 480, ATRASO = 2 * FRAME };

static uint32_t le32(const uint8_t *p) { return p[0] | (p[1] << 8) | (p[2] << 16) | ((uint32_t)p[3] << 24); }
static uint16_t le16(const uint8_t *p) { return p[0] | (p[1] << 8); }

static double agora_us(void) {
  struct timespec t;
  clock_gettime(CLOCK_MONOTONIC, &t);
  return t.tv_sec * 1e6 + t.tv_nsec / 1e3;
}

static int cmp(const void *a, const void *b) {
  double x = *(const double *)a, y = *(const double *)b;
  return (x > y) - (x < y);
}

int main(int argc, char **argv) {
  if (argc < 3) { fprintf(stderr, "uso: %s in.wav out.wav [intensidade] [--tempo]\n", argv[0]); return 2; }
  float intensidade = (argc > 3 && strcmp(argv[3], "--tempo")) ? (float)atof(argv[3]) : 1.0f;
  int medir_tempo = 0;
  for (int i = 3; i < argc; i++) if (!strcmp(argv[i], "--tempo")) medir_tempo = 1;
  FILE *f = fopen(argv[1], "rb");
  if (!f) { perror("entrada"); return 1; }
  fseek(f, 0, SEEK_END); long tam = ftell(f); fseek(f, 0, SEEK_SET);
  uint8_t *buf = malloc(tam);
  if (fread(buf, 1, tam, f) != (size_t)tam) return 1;
  fclose(f);
  long pos = 12; long data_off = -1, data_len = 0; int taxa = 0, canais = 0;
  while (pos + 8 <= tam) {
    uint32_t sz = le32(buf + pos + 4);
    if (!memcmp(buf + pos, "fmt ", 4)) { canais = le16(buf + pos + 10); taxa = (int)le32(buf + pos + 12); }
    if (!memcmp(buf + pos, "data", 4)) { data_off = pos + 8; data_len = sz; break; }
    pos += 8 + sz + (sz & 1);
  }
  if (taxa != 48000 || canais != 1 || data_off < 0) { fprintf(stderr, "so WAV mono 48 kHz PCM16\n"); return 1; }
  if (data_off + data_len > tam) data_len = tam - data_off;
  long n = data_len / 2;
  long quadros = (n + FRAME - 1) / FRAME;
  long total = (quadros + 3) * FRAME;  /* +2 quadros: esvazia a latencia */
  float *in = calloc(total, sizeof(float)), *out = calloc(total, sizeof(float));
  for (long i = 0; i < n; i++) in[i] = (float)(int16_t)le16(buf + data_off + 2 * i);
  DenoiseState *st = rnnoise_create(NULL);
  double *tempos = malloc(sizeof(double) * (quadros + 3));
  for (long q = 0; q < quadros + 2; q++) {
    double t0 = agora_us();
    rnnoise_process_frame(st, out + q * FRAME, in + q * FRAME);
    tempos[q] = agora_us() - t0;
  }
  rnnoise_destroy(st);
  /* saida alinhada: a saida sai com atraso de ATRASO amostras */
  FILE *o = fopen(argv[2], "wb");
  uint8_t h[44]; memcpy(h, buf, 44);
  memcpy(h, "RIFF", 4); memcpy(h + 8, "WAVEfmt ", 8);
  uint32_t v = 16; memcpy(h + 16, &v, 4);
  uint16_t w = 1; memcpy(h + 20, &w, 2); w = 1; memcpy(h + 22, &w, 2);
  v = 48000; memcpy(h + 24, &v, 4); v = 96000; memcpy(h + 28, &v, 4);
  w = 2; memcpy(h + 32, &w, 2); w = 16; memcpy(h + 34, &w, 2);
  memcpy(h + 36, "data", 4); v = (uint32_t)(n * 2); memcpy(h + 40, &v, 4);
  uint32_t riff = 36 + (uint32_t)(n * 2); memcpy(h + 4, &riff, 4);
  fwrite(h, 1, 44, o);
  for (long i = 0; i < n; i++) {
    float molhado = out[i + ATRASO];            /* alinhado: compensa o atraso */
    float seco = in[i];
    float y = seco + intensidade * (molhado - seco);
    if (intensidade <= 0.0f) y = seco;           /* 0%: bytes identicos */
    long r = (long)(y >= 0 ? y + 0.5f : y - 0.5f);
    if (r > 32767) r = 32767; if (r < -32768) r = -32768;
    int16_t s = (int16_t)r; fwrite(&s, 2, 1, o);
  }
  fclose(o);
  if (medir_tempo) {
    long nq = quadros + 2;
    qsort(tempos, nq, sizeof(double), cmp);
    double soma = 0; for (long q = 0; q < nq; q++) soma += tempos[q];
    printf("quadros=%ld media_us=%.1f p50_us=%.1f p99_us=%.1f max_us=%.1f (quadro = 10000 us)\n", nq,
           soma / nq, tempos[nq / 2], tempos[(long)(nq * 0.99)], tempos[nq - 1]);
  }
  return 0;
}
