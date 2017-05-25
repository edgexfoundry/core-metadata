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

import static org.edgexfoundry.test.data.ServiceData.TEST_SERVICE_NAME;
import static org.edgexfoundry.test.data.ServiceData.checkTestData;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.edgexfoundry.Application;
import org.edgexfoundry.dao.AddressableRepository;
import org.edgexfoundry.dao.DeviceServiceDao;
import org.edgexfoundry.dao.DeviceServiceRepository;
import org.edgexfoundry.domain.meta.Addressable;
import org.edgexfoundry.domain.meta.DeviceService;
import org.edgexfoundry.test.category.RequiresMongoDB;
import org.edgexfoundry.test.category.RequiresSpring;
import org.edgexfoundry.test.category.RequiresWeb;
import org.edgexfoundry.test.data.AddressableData;
import org.edgexfoundry.test.data.ServiceData;
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
public class DeviceServiceDaoTest {

	@Autowired
	private DeviceServiceDao dao;

	@Autowired
	private DeviceServiceRepository repos;

	@Autowired
	private AddressableRepository addrRepos;

	private String id;

	@Before
	public void createTestData() {
		Addressable a = AddressableData.newTestInstance();
		addrRepos.save(a);
		DeviceService s = ServiceData.newTestInstance();
		s.setAddressable(a);
		repos.save(s);
		id = s.getId();
		assertNotNull("new test Device Service has no identifier", id);
		assertNotNull("Dao is null", dao);
	}

	@After
	public void cleanup() {
		repos.deleteAll();
		addrRepos.deleteAll();
	}

	@Test
	public void testGetById() {
		DeviceService s = dao.getById(id);
		assertNotNull("Device Service is null on getById", s);
		checkTestData(s, id);
	}

	@Test
	public void testGetByIdWithBadId() {
		assertNull("No device service should be found with bad id", dao.getById("badid"));
	}

	@Test
	public void testGetByName() {
		DeviceService s = dao.getByName(TEST_SERVICE_NAME);
		assertNotNull("Device service is null on getByName", s);
		checkTestData(s, id);
	}

	@Test
	public void testGetByNameWithBadName() {
		assertNull("No device service should be found with bad name", dao.getByName("badname"));
	}

	@Test
	public void testGetByIdOrName() {
		DeviceService s = new DeviceService();
		s.setName(TEST_SERVICE_NAME);
		assertNotNull("Device service is null on getByIdOrName with valid name", dao.getByIdOrName(s));
		s.setName(null);
		s.setId(id);
		assertNotNull("Device service is null on getByIdOrName with valid id", dao.getByIdOrName(s));
	}

	@Test
	public void testGetByIdOrNameWithNull() {
		assertNull("No device service should be found with null on getByIdOrName", dao.getByIdOrName(null));
	}

	@Test
	public void testGetByIdOrNameWithBadIdentifiers() {
		DeviceService s = new DeviceService();
		s.setId("badid");
		assertNull("No device service should be found with bad id on getByIdOrName", dao.getByIdOrName(s));
		s.setId(null);
		s.setName("badname");
		assertNull("No device service should be found with bad name on getByIdOrName", dao.getByIdOrName(s));
	}

}
