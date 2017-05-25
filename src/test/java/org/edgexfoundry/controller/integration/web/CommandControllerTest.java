/*******************************************************************************
 * Copyright 2016-2017 Dell Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * @microservice:  core-metadata
 * @author: Jim White, Dell
 * @version: 1.0.0
 *******************************************************************************/
package org.edgexfoundry.controller.integration.web;

import static org.edgexfoundry.test.data.CommandData.TEST_CMD_NAME;
import static org.edgexfoundry.test.data.CommandData.checkTestData;
import static org.edgexfoundry.test.data.CommandData.newTestInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.edgexfoundry.Application;
import org.edgexfoundry.controller.CommandController;
import org.edgexfoundry.dao.CommandRepository;
import org.edgexfoundry.dao.DeviceProfileRepository;
import org.edgexfoundry.domain.meta.Command;
import org.edgexfoundry.domain.meta.DeviceProfile;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.exception.controller.ServiceException;
import org.edgexfoundry.test.category.RequiresMongoDB;
import org.edgexfoundry.test.category.RequiresSpring;
import org.edgexfoundry.test.category.RequiresWeb;
import org.edgexfoundry.test.data.ProfileData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration("src/test/resources")
@Category({ RequiresMongoDB.class, RequiresSpring.class, RequiresWeb.class })
public class CommandControllerTest {

	private static final String LIMIT = "maxLimit";

	@Autowired
	CommandRepository repos;

	@Autowired
	CommandController controller;

	@Autowired
	DeviceProfileRepository profileRepos;

	private String id;

	@Before
	public void setup() {
		Command c = newTestInstance();
		repos.save(c);
		id = c.getId();
	}

	@After
	public void cleanup() throws Exception {
		resetControllerMAXLIMIT();
		resetRepos();
		profileRepos.deleteAll();
		repos.deleteAll();
	}

	@Test
	public void testCommand() {
		Command c = controller.command(id);
		checkTestData(c, id);
	}

	@Test(expected = NotFoundException.class)
	public void testCommandWithUnknownnId() {
		controller.command("nosuchid");
	}

	@Test(expected = ServiceException.class)
	public void testCommandException() throws Exception {
		unsetRepos();
		controller.command(id);
	}

	@Test
	public void testAddressables() {
		List<Command> cs = controller.commands();
		assertEquals("Find all not returning a list with one command", 1, cs.size());
		checkTestData(cs.get(0), id);
	}

	@Test(expected = ServiceException.class)
	public void testCommandsException() throws Exception {
		unsetRepos();
		controller.commands();
	}

	@Test(expected = LimitExceededException.class)
	public void testCommandssMaxLimitExceeded() throws Exception {
		unsetControllerMAXLIMIT();
		controller.commands();
	}

	@Test
	public void testCommandForName() {
		List<Command> cs = controller.commandForName(TEST_CMD_NAME);
		assertEquals("Find all for name not returning a list with one command", 1, cs.size());
		checkTestData(cs.get(0), id);
	}

	@Test
	public void testCommandForNameWithNoneMatching() {
		List<Command> cs = controller.commandForName("badname");
		assertTrue("Commands found for bad name", cs.isEmpty());
	}

	@Test(expected = ServiceException.class)
	public void testCommandForNameException() throws Exception {
		unsetRepos();
		controller.commandForName(TEST_CMD_NAME);
	}

	@Test
	public void testAdd() {
		Command c = newTestInstance();
		String newId = controller.add(c);
		assertNotNull("New command id is null", newId);
		assertNotNull("Modified date is null", c.getModified());
		assertNotNull("Create date is null", c.getCreated());
	}

	@Test(expected = ServiceException.class)
	public void testAddNull() {
		controller.add(null);
	}

	@Test(expected = ServiceException.class)
	public void testAddException() throws Exception {
		unsetRepos();
		Command c = newTestInstance();
		controller.add(c);
	}

	@Test
	public void testDelete() {
		assertTrue("Delete did not return correctly", controller.delete(id));
	}

	@Test(expected = NotFoundException.class)
	public void testDeleteWithNone() {
		controller.delete("badid");
	}

	@Test(expected = ServiceException.class)
	public void testDeleteException() throws Exception {
		unsetRepos();
		controller.delete(id);
	}

	@Test(expected = DataValidationException.class)
	public void testCommandAssociatedtoDeviceProfile() {
		DeviceProfile p = ProfileData.newTestInstance();
		Command c = repos.findOne(id);
		p.addCommand(c);
		profileRepos.save(p);
		assertNotNull("New profile appears not to have been saved", p.getId());
		controller.delete(id);
	}

	@Test
	public void testUpdate() {
		Command c = repos.findOne(id);
		c.setOrigin(12345);
		assertTrue("Update did not complete successfully", controller.update(c));
		Command c2 = repos.findOne(id);
		assertEquals("Update did not work correclty", 12345, c2.getOrigin());
		assertNotNull("Modified date is null", c2.getModified());
		assertNotNull("Create date is null", c2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				c2.getModified() != c2.getCreated());
	}

	@Test(expected = ServiceException.class)
	public void testUpdateException() throws Exception {
		unsetRepos();
		Command c = repos.findOne(id);
		c.setOrigin(12345);
		controller.update(c);
	}

	@Test(expected = NotFoundException.class)
	public void testUpdateWithNone() {
		Command c = repos.findOne(id);
		c.setId("badid");
		c.setName("badname");
		c.setOrigin(12345);
		controller.update(c);
	}

	private void unsetRepos() throws Exception {
		Class<?> controllerClass = controller.getClass();
		Field temp = controllerClass.getDeclaredField("repos");
		temp.setAccessible(true);
		temp.set(controller, null);
	}

	private void resetRepos() throws Exception {
		Class<?> controllerClass = controller.getClass();
		Field temp = controllerClass.getDeclaredField("repos");
		temp.setAccessible(true);
		temp.set(controller, repos);
	}

	private void unsetControllerMAXLIMIT() throws Exception {
		Class<?> controllerClass = controller.getClass();
		Field temp = controllerClass.getDeclaredField(LIMIT);
		temp.setAccessible(true);
		temp.set(controller, 0);
	}

	private void resetControllerMAXLIMIT() throws Exception {
		Class<?> controllerClass = controller.getClass();
		Field temp = controllerClass.getDeclaredField(LIMIT);
		temp.setAccessible(true);
		temp.set(controller, 1000);
	}
}
