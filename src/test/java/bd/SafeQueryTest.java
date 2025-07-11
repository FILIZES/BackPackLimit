package bd;

import com.filizes.backpacklimit.database.identificator.SQLIdentifier;
import com.filizes.backpacklimit.database.identificator.safe.SafeQuery;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class SafeQueryTest {

    @Test
    void from_shouldFormatQueryCorrectly_withMySQLQuotes() {
        String template = "CREATE TABLE IF NOT EXISTS %s (id INT);";
        SQLIdentifier tableName = SQLIdentifier.of("test_table", s -> "`" + s + "`");
        SafeQuery safeQuery = SafeQuery.from(template, tableName);

        assertEquals("CREATE TABLE IF NOT EXISTS `test_table` (id INT);", safeQuery.sql());
    }

    @Test
    void from_shouldFormatQueryCorrectly_withSQLiteQuotes() {
        String template = "INSERT OR IGNORE INTO %s (uuid) VALUES (?);";
        SQLIdentifier tableName = SQLIdentifier.of("test_table", s -> "\"" + s + "\"");
        SafeQuery safeQuery = SafeQuery.from(template, tableName);

        assertEquals("INSERT OR IGNORE INTO \"test_table\" (uuid) VALUES (?);", safeQuery.sql());
    }
}