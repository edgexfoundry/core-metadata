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

import static org.edgexfoundry.test.data.DeviceData.TEST_NAME;
import static org.edgexfoundry.test.data.DeviceData.newDeviceMgrInstance;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.edgexfoundry.Application;
import org.edgexfoundry.dao.AddressableRepository;
import org.edgexfoundry.dao.DeviceManagerDao;
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
public class DeviceManagerDaoTest {

	@Autowired
	private DeviceManagerDao dao;

	@Autowired
	private DeviceManagerRepository repos;

	@Autowired
	private AddressableRepository addrRepos;

	@Autowired
	private DeviceServiceRepository serviceRepos;

	@Autowired
	private DeviceProfileRepository profileRepos;

	private String id;

	@Before
	public void creatTestData() {
		Addressable a = AddressableData.newTestInstance();
		DeviceManager d = newDeviceMgrInstance();
		addrRepos.save(a);
		d.setAddressable(a);
		DeviceProfile p = new DeviceProfile();
		profileRepos.save(p);
		d.setProfile(p);
		DeviceService s = new DeviceService();
		serviceRepos.save(s);
		d.setService(s);
		repos.save(d);
		id = d.getId();
		assertNotNull("new test DeviceManager has no identifier", id);
		assertNotNull("Dao is null", dao);
	}

	@After
	public void cleanup() {
		repos.deleteAll();
		addrRepos.deleteAll();
		serviceRepos.deleteAll();
		profileRepos.deleteAll();
	}

	@Test
	public void testGetByIdOrName() {
		DeviceManager d = new DeviceManager();
		d.setId(id);
		assertNotNull("DeviceManager is null on getByIdOrName with valid id", dao.getByIdOrName(d));
		d.setName(TEST_NAME);
		d.setId(null);
		assertNotNull("DeviceManager is null on getByIdOrName with valid name", dao.getByIdOrName(d));
	}

	@Test
	public void testGetByIdOrNameWithNull() {
		assertNull("No device manager should be found with null on getByIdOrName", dao.getByIdOrName(null));
	}

	@Test
	public void testGetByIdOrNameWithBadIdentifiers() {
		DeviceManager d = new DeviceManager();
		d.setId("badid");
		assertNull("No device manager should be found with bad id on getByIdOrName", dao.getByIdOrName(d));
		d.setName("badname");
		d.setId(null);
		assertNull("No device manager should be found with bad id on getByIdOrName", dao.getByIdOrName(d));
	}

}
