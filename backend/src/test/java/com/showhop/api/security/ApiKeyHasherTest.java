package com.showhop.api.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ApiKeyHasherTest {

  @Test
  void hashIsDeterministicAndDiffersAcrossKeys() {
    assertThat(ApiKeyHasher.hash("shk_abc")).isEqualTo(ApiKeyHasher.hash("shk_abc"));
    assertThat(ApiKeyHasher.hash("shk_abc")).isNotEqualTo(ApiKeyHasher.hash("shk_xyz"));
  }

  @Test
  void prefixIsTheFirstEightCharactersAfterTheMarker() {
    assertThat(ApiKeyHasher.prefixOf("shk_0123456789abcdef")).isEqualTo("01234567");
  }

  @Test
  void prefixToleratesAKeyShorterThanEightCharactersAfterTheMarker() {
    assertThat(ApiKeyHasher.prefixOf("shk_ab")).isEqualTo("ab");
  }
}
