package com.jay.cli.delegate;

import com.jay.config.model.CliRuntimeOverrides;
import org.junit.jupiter.api.*;

import java.io.File;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TuiDelegatorTest {

    @Test
    void constructorDoesNotThrow() {
        TuiDelegator d = new TuiDelegator();
    }

    @Test
    void delegateSimpleWithNonexistentBinaryThrows() {
        TuiDelegator d = new TuiDelegator(java.nio.file.Path.of("/nonexistent/binary"));
        assertThrows(Exception.class, () ->
                d.delegateSimple("run", List.of()));
    }

    @Test
    void delegateWithFullBridgeDoesNotThrowConstruction() {
        TuiDelegator d = new TuiDelegator();
    }
}

class TuiBinaryLocatorTest {

    @Test
    void locateReturnsPath() {
        var path = TuiBinaryLocator.locate();
        assertNotNull(path);
        assertFalse(path.toString().isBlank());
        assertTrue(path.toString().contains("jay-tui"));
    }
}
