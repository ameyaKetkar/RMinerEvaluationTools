/*
 * Copyright 2010-2014 the original author or authors.
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

package org.springframework.boot.actuate.system;

import java.io.File;
import java.io.FileReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.boot.context.event.ApplicationPreparedEvent;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.mock.env.MockPropertySource;
import org.springframework.util.FileCopyUtils;

import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ApplicationPidFileWriter}.
 *
 * @author Jakub Kubrynski
 * @author Dave Syer
 * @author Phillip Webb
 */
public class ApplicationPidFileWriterTests {

	private static final ApplicationPreparedEvent EVENT = new ApplicationPreparedEvent(
			new SpringApplication(), new String[] {},
			mock(ConfigurableApplicationContext.class));

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Before
	@After
	public void resetListener() {
		System.clearProperty("PIDFILE");
		ApplicationPidFileWriter.reset();
	}

	@Test
	public void createPidFile() throws Exception {
		File file = this.temporaryFolder.newFile();
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
		listener.onApplicationEvent(EVENT);
		assertThat(FileCopyUtils.copyToString(new FileReader(file)), not(isEmptyString()));
	}

	@Test
	public void overridePidFile() throws Exception {
		File file = this.temporaryFolder.newFile();
		System.setProperty("PIDFILE", this.temporaryFolder.newFile().getAbsolutePath());
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
		listener.onApplicationEvent(EVENT);
		assertThat(
				FileCopyUtils.copyToString(new FileReader(System.getProperty("PIDFILE"))),
				not(isEmptyString()));
	}

	@Test
	public void overridePidFileWithSpring() throws Exception {
		File file = this.temporaryFolder.newFile();
		ConfigurableEnvironment environment = new StandardEnvironment();
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.pidfile", file.getAbsolutePath());
		environment.getPropertySources().addLast(propertySource);
		ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
		given(context.getEnvironment()).willReturn(environment);
		ApplicationPreparedEvent event = new ApplicationPreparedEvent(
				new SpringApplication(), new String[] {}, context);
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter();
		listener.onApplicationEvent(event);
		assertThat(FileCopyUtils.copyToString(new FileReader(file)), not(isEmptyString()));
	}

	@Test
	public void differentEventTypes() throws Exception {
		File file = this.temporaryFolder.newFile();
		ConfigurableEnvironment environment = new StandardEnvironment();
		MockPropertySource propertySource = new MockPropertySource();
		propertySource.setProperty("spring.pidfile", file.getAbsolutePath());
		environment.getPropertySources().addLast(propertySource);
		ApplicationEnvironmentPreparedEvent event = new ApplicationEnvironmentPreparedEvent(
				new SpringApplication(), new String[] {}, environment);
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter();
		listener.onApplicationEvent(event);
		assertThat(FileCopyUtils.copyToString(new FileReader(file)), isEmptyString());
		listener.setTriggerEventType(ApplicationEnvironmentPreparedEvent.class);
		listener.onApplicationEvent(event);
		assertThat(FileCopyUtils.copyToString(new FileReader(file)), not(isEmptyString()));
	}

	@Test
	public void withNoEnvironment() throws Exception {
		File file = this.temporaryFolder.newFile();
		ApplicationPidFileWriter listener = new ApplicationPidFileWriter(file);
		listener.setTriggerEventType(ApplicationStartedEvent.class);
		listener.onApplicationEvent(new ApplicationStartedEvent(new SpringApplication(),
				new String[] {}));
		assertThat(FileCopyUtils.copyToString(new FileReader(file)), not(isEmptyString()));
	}

}
