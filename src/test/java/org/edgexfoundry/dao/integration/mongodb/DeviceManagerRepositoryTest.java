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

import static org.edgexfoundry.test.data.AddressableData.TEST_ADDRESS;
import static org.edgexfoundry.test.data.AddressableData.TEST_PORT;
import static org.edgexfoundry.test.data.AddressableData.TEST_PROTOCOL;
import static org.edgexfoundry.test.data.DeviceData.TEST_LABELS;
import static org.edgexfoundry.test.data.DeviceData.TEST_NAME;
import static org.edgexfoundry.test.data.DeviceData.checkTestData;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;

import org.edgexfoundry.Application;
import org.edgexfoundry.dao.AddressableRepository;
import org.edgexfoundry.dao.DeviceManagerRepository;
import org.edgexfoundry.dao.DeviceProfileRepository;
import org.edgexfoundry.dao.DeviceServiceRepository;
import org.edgexfoundry.domain.meta.Addressable;
import org.edgexfoundry.domain.meta.DeviceManager;
import org.edgexfoundry.domain.meta.DeviceProfile;
import org.edgexfoundry.domain.meta.DeviceService;
import org.edgexfoundry.test.category.RequiresMongoDB;
import org.edgexfoundry.test.category.RequiresSpring;
import org.edgexfoundry.test.category.RequiresWeb;
import org.edgexfoundry.test.data.AddressableData;
import org.edgexfoundry.test.data.DeviceData;
import org.edgexfoundry.test.data.ProfileData;
import org.edgexfoundry.test.data.ServiceData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Application.class)
@WebAppConfiguration("src/test/resources")
@Category({ RequiresMongoDB.class, RequiresSpring.class, RequiresWeb.class })
public class DeviceManagerRepositoryTest {

	@Autowired
	private DeviceManagerRepository repos;

	@Autowired
	private AddressableRepository addrRepos;

	@Autowired
	private DeviceProfileRepository profileRepos;

	@Autowired
	private DeviceServiceRepository serviceRepos;

	private String id;
	private String serviceId;
	private String profileId;
	private String addressId;

	/**
	 * Create and save an instance of the DeviceManager before each test Note:
	 * the before method tests the save operation of the Repository
	 */
	@Before
	public void creatTestData() {
		DeviceManager d = DeviceData.newDeviceMgrInstance();
		Addressable a = AddressableData.newTestInstance();
		addrRepos.save(a);
		addressId = a.getId();
		d.setAddressable(a);
		d.setDeviceToo(false);
		DeviceProfile p = ProfileData.newTestInstance();
		profileRepos.save(p);
		profileId = p.getId();
		d.setProfile(p);
		DeviceService s = ServiceData.newTestInstance();
		serviceRepos.save(s);
		serviceId = s.getId();
		d.setService(s);
		repos.save(d);
		id = d.getId();
		assertNotNull("new test Device Manager has no identifier", id);
	}

	@After
	public void cleanup() {
		repos.deleteAll();
		serviceRepos.deleteAll();
		profileRepos.deleteAll();
		addrRepos.deleteAll();
	}

	@Test
	public void testFindOne() {
		DeviceManager mgr = repos.findOne(id);
		assertNotNull("Find one returns no device manager", mgr);
		checkTestData(mgr, id);
	}

	@Test
	public void testFindOneWithBadId() {
		DeviceManager mgr = repos.findOne("foo");
		assertNull("Find one returns device manager with bad id", mgr);
	}

	@Test
	public void testFindAll() {
		List<DeviceManager> as = repos.findAll();
		assertEquals("Find all not returning a list with one device manager", 1, as.size());
		checkTestData(as.get(0), id);
	}

	@Test
	public void testFindByName() {
		DeviceManager mgr = repos.findByName(TEST_NAME);
		assertNotNull("Find by name returns no Device Manager", mgr);
		checkTestData(mgr, id);
	}

	@Test
	public void testFindByNameWithBadName() {
		DeviceManager mgr = repos.findByName("badname");
		assertNull("Find by name returns device manager with bad name", mgr);
	}

	@Test
	public void testFindByLabel() {
		List<DeviceManager> mgrs = repos.findByLabelsIn(TEST_LABELS[0]);
		assertEquals("Find by labels returned no DeviceManager", 1, mgrs.size());
		checkTestData(mgrs.get(0), id);
	}

	@Test
	public void testFindByLabelWithBadLabel() {
		List<DeviceManager> mgrs = repos.findByLabelsIn("foolabel");
		assertTrue("Find by labels returns device manager with bad lable", mgrs.isEmpty());
	}

	@Test
	public void testFindByService() {
		List<DeviceManager> mgrs = repos.findByService(serviceRepos.findOne(serviceId));
		assertEquals("Find by service returned no DeviceManager", 1, mgrs.size());
		checkTestData(mgrs.get(0), id);
	}

	@Test
	public void testFindByServiceWithBadService() {
		DeviceService s = new DeviceService();
		s.setId("abc");
		List<DeviceManager> mgrs = repos.findByService(s);
		assertTrue("Find by service returns device manager with service", mgrs.isEmpty());
	}

	@Test
	public void testFindByProfile() {
		List<DeviceManager> mgrs = repos.findByProfile(profileRepos.findOne(profileId));
		assertEquals("Find by profile returned no DeviceManager", 1, mgrs.size());
		checkTestData(mgrs.get(0), id);
	}

	@Test
	public void testFindByProfileWithBadProfile() {
		DeviceProfile p = new DeviceProfile();
		p.setId("abc");
		List<DeviceManager> mgrs = repos.findByProfile(p);
		assertTrue("Find by profile returns device manager with bad profile", mgrs.isEmpty());
	}

	@Test
	public void testFindByServiceAndName() {
		List<DeviceManager> mgrs = repos.findByServiceAndName(serviceRepos.findOne(serviceId), TEST_NAME);
		assertEquals("Find by service and name returned no DeviceManager", 1, mgrs.size());
		checkTestData(mgrs.get(0), id);
	}

	@Test
	public void testFindByServiceAndNameWithBadServiceorName() {
		DeviceService s = new DeviceService();
		s.setId("abc");
		List<DeviceManager> mgrs = repos.findByServiceAndName(s, TEST_NAME);
		assertTrue("Find by service and name returns device manager with bad service", mgrs.isEmpty());
		mgrs = repos.findByServiceAndName(serviceRepos.findOne(serviceId), "badname");
		assertTrue("Find by service and name returns device manager with bad name", mgrs.isEmpty());
	}

	@Test
	public void testFindByDeviceToo() {
		List<DeviceManager> mgrs = repos.findByDeviceToo(false);
		assertEquals("Find by labels returned no DeviceManager", 1, mgrs.size());
		checkTestData(mgrs.get(0), id);
	}

	@Test
	public void testFindByDeviceTooWithNone() {
		List<DeviceManager> mgrs = repos.findByDeviceToo(true);
		assertTrue("Find by device too returns device manager when it should not have", mgrs.isEmpty());
	}

	@Test
	public void testFindByAddressable() {
		List<DeviceManager> mgrs = repos.findByAddressable(addrRepos.findOne(addressId));
		assertEquals("Find by addressable returned no DeviceManager", 1, mgrs.size());
		checkTestData(mgrs.get(0), id);
	}

	@Test
	public void testFindByAddressableWithBadAddressable() {
		Addressable a = new Addressable("foobar", TEST_PROTOCOL, TEST_ADDRESS, AddressableData.TEST_PATH, TEST_PORT);
		a.setId("abc");
		List<DeviceManager> mgrs = repos.findByAddressable(a);
		assertTrue("Find by addressable returns device manager with bad addressable", mgrs.isEmpty());
	}

	@Test(expected = DuplicateKeyException.class)
	public void testDeviceManagerWithSameName() {
		DeviceManager d = new DeviceManager();
		d.setName(TEST_NAME);
		repos.save(d);
		fail("Should not have been able to save the device manager with a duplicate name");
	}

	@Test
	public void testUpdate() {
		DeviceManager mgr = repos.findOne(id);
		// check that create and modified timestamps are the same
		assertEquals("Modified and created timestamps should be equal after creation", mgr.getModified(),
				mgr.getCreated());
		mgr.setDescription("new description");
		repos.save(mgr);
		// reread device manager
		DeviceManager mgr2 = repos.findOne(id);
		assertEquals("Device manager was not updated appropriately", "new description", mgr2.getDescription());
		assertNotEquals(
				"after modification, modified timestamp still the same as the device manager's create timestamp",
				mgr2.getModified(), mgr2.getCreated());
	}

	@Test
	public void testDelete() {
		DeviceManager mgr = repos.findOne(id);
		repos.delete(mgr);
		assertNull("Device manager not deleted", repos.findOne(id));
	}

}
