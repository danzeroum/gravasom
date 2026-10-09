/* O RNNoise v0.2 inclui "os_support.h" (herdado do Opus) nos caminhos NEON e escalar, mas nao o distribui; so o caminho x86 (AVX)
 * dispensa. Aqui entra o minimo que esse cabecalho daria: OPUS_CLEAR sobre o RNN_CLEAR do proprio RNNoise (common.h). */
#ifndef GRAVASOM_OS_SUPPORT_SHIM_H
#define GRAVASOM_OS_SUPPORT_SHIM_H
#include "common.h"
#ifndef OPUS_CLEAR
#define OPUS_CLEAR(dst, n) RNN_CLEAR(dst, n)
#endif
#endif
