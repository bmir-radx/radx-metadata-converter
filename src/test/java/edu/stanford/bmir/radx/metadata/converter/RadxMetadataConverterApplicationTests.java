package edu.stanford.bmir.radx.metadata.converter;

import org.junit.jupiter.api.Test;
import picocli.CommandLine;
import static org.junit.jupiter.api.Assertions.*;

class RadxMetadataConverterApplicationTests {

	@Test
	void converterCommandCanBeInstantiated() {
		// Test that the ConverterCommand can be instantiated and picocli can process it
		assertDoesNotThrow(() -> {
			var command = new ConverterCommand();
			var commandLine = new CommandLine(command);
			// Verify the command has the expected name
			assertEquals("convertMetadataInstances", commandLine.getCommandName());
		});
	}

}
