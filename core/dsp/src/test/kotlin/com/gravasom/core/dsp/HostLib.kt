package com.gravasom.core.dsp

import org.junit.Assume.assumeTrue

/** Os testes de JNI no host so rodam com a lib de host (scripts/build-dsp-host.sh) apontada por GRAVASOM_DSP_HOST_LIB. */
internal fun assumeHostLib() {
    val configured = System.getenv("GRAVASOM_DSP_HOST_LIB") != null
    assumeTrue("libgravasom_dsp de host indisponivel", configured || System.getProperty("java.library.path").orEmpty().contains("dsp-host"))
}
