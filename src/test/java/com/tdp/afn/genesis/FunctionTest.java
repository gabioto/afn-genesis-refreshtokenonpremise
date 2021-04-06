package com.tdp.afn.genesis;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.logging.Logger;

import com.microsoft.azure.functions.ExecutionContext;

import org.junit.jupiter.api.Test;

/**
 * Unit test for Function class.
 */
public class FunctionTest {

    @Test
    public void caso_ok() throws Exception {
        // Setup
        final ExecutionContext context = mock(ExecutionContext.class);
        doReturn(Logger.getGlobal()).when(context).getLogger();

        // Invoke
        final String ret = new Function().run(null, context);

        // Verify
        assertEquals(ret, "OK");
    }
}
