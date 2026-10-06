package com.buildflow;

import com.buildflow.audit.service.AuditValueRedactor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditValueRedactorTest {

    @Test
    void emptyValuesStayAsTheyAre() {

        assertThat(AuditValueRedactor.redact(null)).isNull();
        assertThat(AuditValueRedactor.redact("")).isEqualTo("");
    }

    @Test
    void ordinaryValuesAreNotChanged() {

        String value =
            "TaskView[id=7, title=Pour slab, status=DONE, assignee=Ann]";

        assertThat(AuditValueRedactor.redact(value)).isEqualTo(value);
    }

    @Test
    void passwordLikeFieldsInARecordAreMasked() {

        assertThat(
            AuditValueRedactor.redact(
                "UserView[id=1, email=a@b.c, passwordHash=$2a$10$abc, name=Ann]"
            )
        ).isEqualTo(
            "UserView[id=1, email=a@b.c, passwordHash=[redacted], name=Ann]"
        );
    }

    @Test
    void theLastFieldBeforeABracketIsMaskedToo() {

        assertThat(
            AuditValueRedactor.redact("Login[email=a@b.c, token=abc123]")
        ).isEqualTo(
            "Login[email=a@b.c, token=[redacted]]"
        );
    }

    @Test
    void nestedRecordsAreMasked() {

        assertThat(
            AuditValueRedactor.redact(
                "Result[user=User[id=1, passwordHash=abc], count=2]"
            )
        ).isEqualTo(
            "Result[user=User[id=1, passwordHash=[redacted]], count=2]"
        );
    }

    @Test
    void jsonStringValuesAreMasked() {

        assertThat(
            AuditValueRedactor.redact(
                "{\"email\":\"a@b.c\",\"password\":\"p@ss, word\",\"n\":1}"
            )
        ).isEqualTo(
            "{\"email\":\"a@b.c\",\"password\":\"[redacted]\",\"n\":1}"
        );
    }

    @Test
    void jsonNumbersAreMaskedToo() {

        assertThat(
            AuditValueRedactor.redact("{\"apiKey\": 12345, \"x\": 1}")
        ).isEqualTo(
            "{\"apiKey\": \"[redacted]\", \"x\": 1}"
        );
    }

    @Test
    void fieldNamesAreMatchedWithoutRegardToCase() {

        assertThat(
            AuditValueRedactor.redact("Session[ACCESS_TOKEN=abc, Secret=xyz]")
        ).isEqualTo(
            "Session[ACCESS_TOKEN=[redacted], Secret=[redacted]]"
        );
    }

    @Test
    void specialCharactersInAValueAreHandled() {

        assertThat(
            AuditValueRedactor.redact("Login[password=$1\\2]")
        ).isEqualTo(
            "Login[password=[redacted]]"
        );
    }

    @Test
    void maskingTwiceGivesTheSameResult() {

        String once =
            AuditValueRedactor.redact(
                "Login[token=abc, note=hello, \"password\": \"x\"]"
            );

        assertThat(AuditValueRedactor.redact(once)).isEqualTo(once);
    }
}
