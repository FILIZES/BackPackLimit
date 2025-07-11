package bd;

import com.filizes.backpacklimit.utils.ValidationUtils;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ValidationUtilsTest {

    @Test
    void isValidTableName_shouldReturnTrue_forValidNames() {
        assertTrue(ValidationUtils.isValidTableName("valid_table"));
        assertTrue(ValidationUtils.isValidTableName("table123"));
        assertTrue(ValidationUtils.isValidTableName("_table"));
    }

    @Test
    void isValidTableName_shouldReturnFalse_forInvalidNames() {
        assertFalse(ValidationUtils.isValidTableName("1table"));
        assertFalse(ValidationUtils.isValidTableName("table-name"));
        assertFalse(ValidationUtils.isValidTableName("table name"));
        assertFalse(ValidationUtils.isValidTableName(""));
        assertFalse(ValidationUtils.isValidTableName(null));
    }
}