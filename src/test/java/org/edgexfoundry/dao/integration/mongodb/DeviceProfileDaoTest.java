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
package org.edgexfoundry.dao.integration.mongodb;

import static org.edgexfoundry.test.data.ProfileData.TEST_PROFILE_NAME;
import static org.edgexfoundry.test.data.ProfileData.checkTestData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.edgexfoundry.Application;
import org.edgexfoundry.dao.CommandRepository;
import org.edgexfoundry.dao.DeviceProfileDao;
import org.edgexfoundry.dao.DeviceProfileRepository;
import org.edgexfoundry.dao.DeviceRepository;
import org.edgexfoundry.dao.DeviceServiceRepository;
import org.edgexfoundry.domain.meta.Command;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.domain.meta.DeviceProfile;
import org.edgexfoundry.domain.meta.DeviceService;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.test.category.RequiresMongoDB;
import org.edgexfoundry.test.category.RequiresSpring;
import org.edgexfoundry.test.category.RequiresWeb;
import org.edgexfoundry.test.data.CommandData;
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
public class DeviceProfileDaoTest {

	@Autowired
	private DeviceProfileDao dao;

	@Autowired
	private DeviceProfileRepository repos;

	@Autowired
	private CommandRepository cmdRepos;

	@Autowired
	private DeviceServiceRepository serviceRepos;

	@Autowired
	private DeviceRepository deviceRepos;

	private String id;

	@Before
	public void createTestData() {
		DeviceProfile p = ProfileData.newTestInstance();
		Command c = CommandData.newTestInstance();
		cmdRepos.save(c);
		List<Command> commands = new ArrayList<Command>();
		commands.add(c);
		p.setCommands(commands);
		repos.save(p);
		id = p.getId();
		assertNotNull("new test Device Profile has no identifier", id);
		assertNotNull("Dao is null", dao);
	}

	@After
	public void cleanup() {
		repos.deleteAll();
		cmdRepos.deleteAll();
		deviceRepos.deleteAll();
		serviceRepos.deleteAll();
	}

	@Test
	public void testGetById() {
		DeviceProfile p = dao.getById(id);
		assertNotNull("Device Profile is null on getById", p);
		checkTestData(p, id);
	}

	@Test
	public void testGetByIdWithBadId() {
		assertNull("No device profile should be found with bad id", dao.getById("badid"));
	}

	@Test
	public void testGetByName() {
		DeviceProfile p = dao.getByName(TEST_PROFILE_NAME);
		assertNotNull("Device profile is null on getByName", p);
		checkTestData(p, id);
	}

	@Test
	public void testGetByNameWithBadName() {
		assertNull("No device profile should be found with bad name", dao.getByName("badname"));
	}

	@Test
	public void testGetByIdOrName() {
		DeviceProfile p = new DeviceProfile();
		p.setName(TEST_PROFILE_NAME);
		assertNotNull("Device profile is null on getByIdOrName with valid name", dao.getByIdOrName(p));
		p.setName(null);
		p.setId(id);
		assertNotNull("Device profile is null on getByIdOrName with valid id", dao.getByIdOrName(p));
	}

	@Test
	public void testGetByIdOrNameWithNull() {
		assertNull("No device profile should be found with null on getByIdOrName", dao.getByIdOrName(null));
	}

	@Test
	public void testGetByIdOrNameWithBadIdentifiers() {
		DeviceProfile p = new DeviceProfile();
		p.setId("badid");
		assertNull("No device profile should be found with bad id on getByIdOrName", dao.getByIdOrName(p));
		p.setId(null);
		p.setName("badname");
		assertNull("No device profile should be found with bad name on getByIdOrName", dao.getByIdOrName(p));
	}

	@Test
	public void testGetOwningServices() {
		DeviceProfile p = repos.findOne(id);
		DeviceService service = new DeviceService();
		serviceRepos.save(service);
		Device device = new Device();
		device.setService(service);
		device.setProfile(p);
		deviceRepos.save(device);
		assertEquals("Did not find correct number of associated DeviceServices ", 1, dao.getOwningServices(p).size());
	}

	@Test
	public void testGetOwningServicesHavingNone() {
		assertTrue("Should be no owning device services", dao.getOwningServices(repos.findOne(id)).isEmpty());
	}

	@Test
	public void testGetOwningServicesWithNull() {
		assertTrue("Should be no owning device services", dao.getOwningServices(null).isEmpty());
	}

	@Test
	public void testCheckCommandNames() {
		DeviceProfile p = repos.findOne(id);
		// if there is a problem with names, this should throw an exception
		dao.checkCommandNames(p.getCommands());
	}

	@Test(expected = DataValidationException.class)
	public void testCheckCommandNamesWithDupNames() {
		DeviceProfile p = repos.findOne(id);
		Command c2 = CommandData.newTestInstance();
		p.addCommand(c2);
		dao.checkCommandNames(p.getCommands());
	}

	@Test
	public void testGetAssociatedProfilesForCommand() {
		Command c = repos.findOne(id).getCommands().get(0);
		assertNotNull("Command needs to be associated to profile for test", c);
		List<DeviceProfile> assocs = dao.getAssociatedProfilesForCommand(c);
		assertEquals("List of associated profiles to command is incorrect", 1, assocs.size());
		assertEquals("Profile returned as associate to command is not a match", id, assocs.get(0).getId());
	}

	@Test
	public void testGetAssociatedProfileForNullCommand() {
		assertTrue("Device profiles are returned for null command association",
				dao.getAssociatedProfilesForCommand(null).isEmpty());
	}

	@Test
	public void testGetAssociatedProfileForProfileWithNoCommand() {
		DeviceProfile p = repos.findOne(id);
		Command c = p.getCommands().get(0);
		p.setCommands(null);
		repos.save(p);
		assertTrue("Device profiles are returned for profile with no command association",
				dao.getAssociatedProfilesForCommand(c).isEmpty());
	}

}
