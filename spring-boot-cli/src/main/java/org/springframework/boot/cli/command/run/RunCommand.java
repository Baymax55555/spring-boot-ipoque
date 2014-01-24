/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.cli.command.run;

import java.awt.Desktop;
import java.io.File;
import java.util.List;
import java.util.logging.Level;

import joptsimple.OptionSet;
import joptsimple.OptionSpec;

import org.springframework.boot.cli.command.Command;
import org.springframework.boot.cli.command.CompilerOptionHandler;
import org.springframework.boot.cli.command.OptionParsingCommand;
import org.springframework.boot.cli.command.SourceOptions;
import org.springframework.boot.cli.compiler.GroovyCompilerConfigurationAdapter;
import org.springframework.boot.cli.compiler.GroovyCompilerScope;
import org.springframework.boot.cli.compiler.RepositoryConfigurationFactory;
import org.springframework.boot.cli.compiler.grape.RepositoryConfiguration;

import static java.util.Arrays.asList;

/**
 * {@link Command} to 'run' a groovy script or scripts.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 * @author Andy Wilkinson
 * @see SpringApplicationRunner
 */
public class RunCommand extends OptionParsingCommand {

	public RunCommand() {
		super("run", "Run a spring groovy script", new RunOptionHandler());
	}

	@Override
	public String getUsageHelp() {
		return "[options] <files> [--] [args]";
	}

	public void stop() {
		if (this.getHandler() != null) {
			((RunOptionHandler) this.getHandler()).stop();
		}
	}

	private static class RunOptionHandler extends CompilerOptionHandler {

		private OptionSpec<Void> watchOption;

		private OptionSpec<Void> editOption;

		private OptionSpec<Void> verboseOption;

		private OptionSpec<Void> quietOption;

		private SpringApplicationRunner runner;

		@Override
		protected void doOptions() {
			this.watchOption = option("watch", "Watch the specified file for changes");
			this.editOption = option(asList("edit", "e"),
					"Open the file with the default system editor");
			this.verboseOption = option(asList("verbose", "v"),
					"Verbose logging of dependency resolution");
			this.quietOption = option(asList("quiet", "q"), "Quiet logging");
		}

		public synchronized void stop() {
			if (this.runner != null) {
				this.runner.stop();
			}
			this.runner = null;
		}

		@Override
		protected synchronized void run(OptionSet options) throws Exception {

			if (this.runner != null) {
				throw new RuntimeException(
						"Already running. Please stop the current application before running another (use the 'stop' command).");
			}

			SourceOptions sourceOptions = new SourceOptions(options);

			if (options.has(this.editOption)) {
				Desktop.getDesktop().edit(new File(sourceOptions.getSources().get(0)));
			}

			List<RepositoryConfiguration> repositoryConfiguration = RepositoryConfigurationFactory
					.createDefaultRepositoryConfiguration();
			repositoryConfiguration.add(0, new RepositoryConfiguration("local", new File(
					"repository").toURI(), true));

			SpringApplicationRunnerConfiguration configuration = new SpringApplicationRunnerConfigurationAdapter(
					options, this, repositoryConfiguration);

			this.runner = new SpringApplicationRunner(configuration,
					sourceOptions.getSourcesArray(), sourceOptions.getArgsArray());
			this.runner.compileAndRun();
		}

		/**
		 * Simple adapter class to present the {@link OptionSet} as a
		 * {@link SpringApplicationRunnerConfiguration}.
		 */
		private class SpringApplicationRunnerConfigurationAdapter extends
				GroovyCompilerConfigurationAdapter implements
				SpringApplicationRunnerConfiguration {

			public SpringApplicationRunnerConfigurationAdapter(OptionSet options,
					CompilerOptionHandler optionHandler,
					List<RepositoryConfiguration> repositoryConfiguration) {
				super(options, optionHandler, repositoryConfiguration);
			}

			@Override
			public GroovyCompilerScope getScope() {
				return GroovyCompilerScope.DEFAULT;
			}

			@Override
			public boolean isWatchForFileChanges() {
				return getOptions().has(RunOptionHandler.this.watchOption);
			}

			@Override
			public Level getLogLevel() {
				if (getOptions().has(RunOptionHandler.this.quietOption)) {
					return Level.OFF;
				}
				if (getOptions().has(RunOptionHandler.this.verboseOption)) {
					return Level.FINEST;
				}
				return Level.INFO;
			}
		}
	}

}
