package edu.stanford.bmir.radx.metadata.converter;

import picocli.CommandLine;

public class RadxMetadataConverterApplication {

	public static void main(String[] args) {
		var command = new ConverterCommand();
		int exitCode = new CommandLine(command).execute(args);
		System.exit(exitCode);
	}
}
