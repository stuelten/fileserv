package de.sty.fileserv.auth.file.smb.utils;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SmbUtilsTest {
    @Test
    void testNtlmHash() {
        // 'password' NTLM hash
        assertThat(SmbUtils.ntlmHash("password")).isEqualTo("8846F7EAEE8FB117AD06BDD830B7586C");
    }

}
