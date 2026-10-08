# WebRTC VAD (detecção de voz)

- Origem: sdist `webrtcvad 2.0.10` (PyPI), sha256 em `core/dsp/src/main/cpp/webrtc_vad/VENDOR.sha256`. Subconjunto vendorizado **sem alterações**:
  `common_audio/vad/*` e `common_audio/signal_processing/*` (mais os cabeçalhos `typedefs.h`). O invólucro do Gravasom é `vad_stream.cpp`/`vad_jni.cpp`.
- Licença do código WebRTC: BSD de 3 cláusulas.

```
Copyright (c) 2011, The WebRTC project authors. All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

  * Redistributions of source code must retain the above copyright
    notice, this list of conditions and the following disclaimer.

  * Redistributions in binary form must reproduce the above copyright
    notice, this list of conditions and the following disclaimer in
    the documentation and/or other materials provided with the
    distribution.

  * Neither the name of Google nor the names of its contributors may
    be used to endorse or promote products derived from this software
    without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
```

- Empacotamento Python `py-webrtcvad` (usado só para obter a fonte): MIT, Copyright (c) 2016 John Wiseman — texto em
  `core/dsp/src/main/cpp/webrtc_vad/LICENSE-py-webrtcvad-MIT`.
- Os arquivos do WebRTC citam `AUTHORS` e `PATENTS` da árvore original; o concessão adicional de patentes do WebRTC vale para o uso
  neste app (veja https://webrtc.googlesource.com/src/+/refs/heads/main/PATENTS).
