package com.campusconnect.profile.service;

import com.campusconnect.common.exception.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SkillNamesTest {

    @Test
    void normalize_trimsLowercasesAndCollapsesSpaces() {
        assertThat(SkillNames.normalize("  Spring    BOOT ")).isEqualTo("spring boot");
    }

    @ParameterizedTest
    @ValueSource(strings = {"c++", "C#", ".NET", "Node.js", "scikit-learn"})
    void realWorldSkillNames_areAccepted(String raw) {
        assertThat(SkillNames.normalize(raw)).isEqualTo(raw.toLowerCase());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "a/b", "drop table;", "50%", "under_score", "🙂"})
    void junk_isRejected(String raw) {
        assertThatThrownBy(() -> SkillNames.normalize(raw))
                .isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo("INVALID_SKILL"));
    }

    @Test
    void over50Characters_isRejected() {
        assertThatThrownBy(() -> SkillNames.normalize("a".repeat(51))).isInstanceOf(ApiException.class);
    }

    @Test
    void normalizeAll_removesDuplicates_keepingFirstOrder() {
        assertThat(SkillNames.normalizeAll(List.of("Java", "python", " JAVA ", "Python")))
                .containsExactly("java", "python");
    }
}