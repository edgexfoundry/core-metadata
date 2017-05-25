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

import static org.edgexfoundry.test.data.DeviceData.TEST_LABELS;
import static org.edgexfoundry.test.data.DeviceData.TEST_NAME;
import static org.edgexfoundry.test.data.DeviceData.checkTestData;
import static org.edgexfoundry.test.data.DeviceData.newDeviceMgrInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.edgexfoundry.Application;
import org.edgexfoundry.controller.DeviceManagerController;
import org.edgexfoundry.dao.AddressableRepository;
import org.edgexfoundry.dao.DeviceManagerRepository;
import org.edgexfoundry.dao.DeviceProfileRepository;
import org.edgexfoundry.dao.DeviceServiceRepository;
import org.edgexfoundry.domain.meta.Addressable;
import org.edgexfoundry.domain.meta.AdminState;
import org.edgexfoundry.domain.meta.DeviceManager;
import org.edgexfoundry.domain.meta.DeviceProfile;
import org.edgexfoundry.domain.meta.DeviceService;
import org.edgexfoundry.domain.meta.OperatingState;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.exception.controller.ServiceException;
import org.edgexfoundry.test.category.RequiresMongoDB;
import org.edgexfoundry.test.category.RequiresSpring;
import org.edgexfoundry.test.category.RequiresWeb;
import org.edgexfoundry.test.data.AddressableData;
import org.edgexfoundry.test.data.ProfileData;
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
public class DeviceManagerControllerTest {

	private static final String LIMIT = "maxLimit";

	@Autowired
	DeviceManagerRepository repos;

	@Autowired
	DeviceManagerController controller;

	@Autowired
	AddressableRepository addrRepos;

	@Autowired
	DeviceServiceRepository serviceRepos;

	@Autowired
	DeviceProfileRepository profileRepos;

	private String id;
	private String addrId;
	private String serviceId;
	private String profileId;

	@Before
	public void setup() {
		DeviceService service = ServiceData.newTestInstance();
		serviceRepos.save(service);
		serviceId = service.getId();
		DeviceProfile profile = ProfileData.newTestInstance();
		profileRepos.save(profile);
		profileId = profile.getId();
		Addressable a = AddressableData.newTestInstance();
		addrRepos.save(a);
		addrId = a.getId();
		DeviceManager d = newDeviceMgrInstance();
		d.setAddressable(a);
		d.setService(service);
		d.setProfile(profile);
		repos.save(d);
		id = d.getId();
	}

	@After
	public void cleanup() throws Exception {
		resetControllerMAXLIMIT();
		resetRepos();
		addrRepos.deleteAll();
		serviceRepos.deleteAll();
		profileRepos.deleteAll();
		repos.deleteAll();
	}

	@Test
	public void testDeviceManager() {
		DeviceManager d = controller.deviceManager(id);
		checkTestData(d, id);
	}

	@Test(expected = NotFoundException.class)
	public void testDeviceManagerWithUnknownnId() {
		controller.deviceManager("nosuchid");
	}

	@Test(expected = ServiceException.class)
	public void testDeviceManagerException() throws Exception {
		unsetRepos();
		controller.deviceManager(id);
	}

	@Test
	public void testDeviceManagers() {
		List<DeviceManager> as = controller.deviceManagers();
		assertEquals("Find all not returning a list with one device", 1, as.size());
		checkTestData(as.get(0), id);
	}

	@Test(expected = ServiceException.class)
	public void testDeviceManagersException() throws Exception {
		unsetRepos();
		controller.deviceManagers();
	}

	@Test(expected = LimitExceededException.class)
	public void testDeviceManagersMaxLimitExceeded() throws Exception {
		unsetControllerMAXLIMIT();
		controller.deviceManagers();
	}

	@Test
	public void testDeviceManagerForName() {
		DeviceManager a = controller.deviceManagerForName(TEST_NAME);
		checkTestData(a, id);
	}

	@Test(expected = NotFoundException.class)
	public void testDeviceManagerForNameWithNoneMatching() {
		controller.deviceManagerForName("badname");
	}

	@Test(expected = ServiceException.class)
	public void testDeviceManagerForNameException() throws Exception {
		unsetRepos();
		controller.deviceManagerForName(TEST_NAME);
	}

	@Test
	public void testDeviceManagerByLabel() {
		List<DeviceManager> ds = controller.deviceManagerByLabel(TEST_LABELS[0]);
		assertEquals("Find for labels not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test
	public void testDeviceManagersByLabelWithNoneMatching() {
		assertTrue("No device managers should be found with bad label",
				controller.deviceManagerByLabel("badlabel").isEmpty());
	}

	@Test(expected = ServiceException.class)
	public void testDeviceManagersByLabelException() throws Exception {
		unsetRepos();
		controller.deviceManagerByLabel(TEST_LABELS[0]);
	}

	@Test
	public void testDeviceManagersForAddressable() {
		List<DeviceManager> ds = controller.deviceManagersForAddressable(addrId);
		assertEquals("Find for address not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test
	public void testDeviceManagersForAddressableByName() {
		List<DeviceManager> ds = controller.deviceManagersForAddressableByName(AddressableData.TEST_ADDR_NAME);
		assertEquals("Find for address not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test(expected = NotFoundException.class)
	public void testDeviceManagersForAddressableWithNoneMatching() throws Exception {
		controller.deviceManagersForAddressable("badaddress");
	}

	@Test(expected = NotFoundException.class)
	public void testDeviceManagersForAddressableByNameWithNoneMatching() throws Exception {
		controller.deviceManagersForAddressableByName("badaddress");
	}

	@Test(expected = ServiceException.class)
	public void testDeviceManagersForAddressException() throws Exception {
		unsetRepos();
		controller.deviceManagersForAddressable(addrId);
	}

	@Test
	public void testDeviceManagersForService() {
		List<DeviceManager> ds = controller.deviceManagersForService(serviceId);
		assertEquals("Find for services not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test
	public void testDeviceManagersForServiceByName() {
		List<DeviceManager> ds = controller.deviceManagersForServiceByName(ServiceData.TEST_SERVICE_NAME);
		assertEquals("Find for services not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test(expected = NotFoundException.class)
	public void testDeviceManagersForServiceWithNone() {
		controller.deviceManagersForService("badservice");
	}

	@Test(expected = NotFoundException.class)
	public void testDeviceManagersForServiceByNameWithNone() {
		controller.deviceManagersForServiceByName("badservice");
	}

	@Test(expected = ServiceException.class)
	public void testDeviceManagersForServiceException() throws Exception {
		unsetRepos();
		controller.deviceManagersForService(serviceId);
	}

	@Test
	public void testDeviceManagersForProfile() {
		List<DeviceManager> ds = controller.deviceManagersForProfile(profileId);
		assertEquals("Find for profiles not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test
	public void testDeviceManagersForProfileByName() {
		List<DeviceManager> ds = controller.deviceManagersForProfileByName(ProfileData.TEST_PROFILE_NAME);
		assertEquals("Find for profiles not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test(expected = NotFoundException.class)
	public void testDeviceManagersForProfileWithNone() {
		assertTrue("No device managers should be found with bad profile",
				controller.deviceManagersForProfile("badprofile").isEmpty());
	}

	@Test(expected = NotFoundException.class)
	public void testDeviceManagersForProfileByNameWithNone() {
		controller.deviceManagersForProfileByName("badprofile");
	}

	@Test(expected = ServiceException.class)
	public void testDeviceManagersForProfileException() throws Exception {
		unsetRepos();
		controller.deviceManagersForProfile(profileId);
	}

	@Test
	public void testAdd() {
		DeviceManager d = repos.findOne(id);
		d.setId(null);
		d.setName("NewName");
		String newId = controller.add(d);
		assertNotNull("New device manager id is null", newId);
		assertNotNull("Modified date is null", d.getModified());
		assertNotNull("Create date is null", d.getCreated());
	}

	@Test(expected = DataValidationException.class)
	public void testAddWithNullAdminState() {
		DeviceManager d = repos.findOne(id);
		d.setId(null);
		d.setName("NewName");
		d.setAdminState(null);
		controller.add(d);
	}

	@Test(expected = DataValidationException.class)
	public void testAddWithNullOpState() {
		DeviceManager d = repos.findOne(id);
		d.setId(null);
		d.setName("NewName");
		d.setOperatingState(null);
		controller.add(d);
	}

	@Test(expected = ServiceException.class)
	public void testAddNull() {
		controller.add(null);
	}

	@Test(expected = DataValidationException.class)
	public void testAddWithSameName() {
		DeviceManager d = repos.findOne(id);
		d.setId(null);
		controller.add(d);
	}

	@Test(expected = DataValidationException.class)
	public void testAddWithNoDeviceManagerService() {
		DeviceManager d = repos.findOne(id);
		d.setId(null);
		d.setName("newname");
		d.setService(null);
		controller.add(d);
	}

	@Test(expected = DataValidationException.class)
	public void testAddWithNoDeviceManagerProfile() {
		DeviceManager d = repos.findOne(id);
		d.setId(null);
		d.setName("newname");
		d.setProfile(null);
		controller.add(d);
	}

	@Test(expected = DataValidationException.class)
	public void testAddWithNoAddressable() {
		DeviceManager d = repos.findOne(id);
		d.setId(null);
		d.setName("newname");
		d.setAddressable(null);
		controller.add(d);
	}

	@Test(expected = ServiceException.class)
	public void testAddException() throws Exception {
		unsetRepos();
		DeviceManager d = repos.findOne(id);
		d.setId(null);
		d.setName("NewName");
		controller.add(d);
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

	@Test
	public void testDeleteByName() {
		assertTrue("Delete did not return correctly", controller.deleteByName(TEST_NAME));
	}

	@Test(expected = NotFoundException.class)
	public void testDeleteByNameWithNone() {
		controller.delete("badname");
	}

	@Test(expected = ServiceException.class)
	public void testDeleteByNameException() throws Exception {
		unsetRepos();
		controller.deleteByName(TEST_NAME);
	}

	@Test
	public void testUpdate() {
		DeviceManager d = repos.findOne(id);
		d.setDescription("newdescription");
		assertTrue("Update did not complete successfully", controller.update(d));
		DeviceManager d2 = repos.findOne(id);
		assertEquals("Update did not work correclty", "newdescription", d2.getDescription());
		assertNotNull("Modified date is null", d2.getModified());
		assertNotNull("Create date is null", d2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				d2.getModified() != d2.getCreated());
	}

	@Test
	public void testUpdateLastConnected() {
		assertTrue("Update did not complete successfully", controller.updateLastConnected(id, 1000));
		DeviceManager d2 = repos.findOne(id);
		assertEquals("Update last connected did not work correclty", 1000, d2.getLastConnected());
		assertNotNull("Modified date is null", d2.getModified());
		assertNotNull("Create date is null", d2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				d2.getModified() != d2.getCreated());
	}

	@Test(expected = NotFoundException.class)
	public void testUpdateLastConnectedNoneFound() {
		controller.updateLastConnected("badid", 1000);
	}

	@Test(expected = ServiceException.class)
	public void testUpdateLastConnectedException() throws Exception {
		unsetRepos();
		controller.updateLastConnected(id, 1000);
	}

	@Test
	public void testUpdateLastConnectedByName() {
		assertTrue("Update did not complete successfully", controller.updateLastConnectedByName(TEST_NAME, 1000));
		DeviceManager d2 = repos.findByName(TEST_NAME);
		assertEquals("Update last connected did not work correclty", 1000, d2.getLastConnected());
		assertNotNull("Modified date is null", d2.getModified());
		assertNotNull("Create date is null", d2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				d2.getModified() != d2.getCreated());
	}

	@Test(expected = NotFoundException.class)
	public void testUpdateLastConnectedByNameNoneFound() {
		controller.updateLastConnectedByName("badname", 1000);
	}

	@Test(expected = ServiceException.class)
	public void testUpdateLastConnectedByNameException() throws Exception {
		unsetRepos();
		controller.updateLastConnectedByName(TEST_NAME, 1000);
	}

	@Test
	public void testUpdateLastReported() {
		assertTrue("Update did not complete successfully", controller.updateLastReported(id, 1000));
		DeviceManager d2 = repos.findOne(id);
		assertEquals("Update last reported did not work correclty", 1000, d2.getLastReported());
		assertNotNull("Modified date is null", d2.getModified());
		assertNotNull("Create date is null", d2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				d2.getModified() != d2.getCreated());
	}

	@Test(expected = NotFoundException.class)
	public void testUpdateLastReportedNoneFound() {
		controller.updateLastReported("badid", 1000);
	}

	@Test(expected = ServiceException.class)
	public void testUpdateLastReportedException() throws Exception {
		unsetRepos();
		controller.updateLastReported(id, 1000);
	}

	@Test
	public void testUpdateLastReportedByName() {
		assertTrue("Update did not complete successfully", controller.updateLastReportedByName(TEST_NAME, 1000));
		DeviceManager d2 = repos.findByName(TEST_NAME);
		assertEquals("Update last reported did not work correclty", 1000, d2.getLastReported());
		assertNotNull("Modified date is null", d2.getModified());
		assertNotNull("Create date is null", d2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				d2.getModified() != d2.getCreated());
	}

	@Test(expected = NotFoundException.class)
	public void testUpdateLastReportedByNameNoneFound() {
		controller.updateLastReportedByName("badname", 1000);
	}

	@Test(expected = ServiceException.class)
	public void testUpdateLastReportedByNameException() throws Exception {
		unsetRepos();
		controller.updateLastReportedByName(TEST_NAME, 1000);
	}

	@Test
	public void testUpdateOpState() {
		assertTrue("Update did not complete successfully",
				controller.updateOpState(id, OperatingState.disabled.toString()));
		DeviceManager d2 = repos.findOne(id);
		assertEquals("Update op state did not work correclty", OperatingState.disabled, d2.getOperatingState());
		assertNotNull("Modified date is null", d2.getModified());
		assertNotNull("Create date is null", d2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				d2.getModified() != d2.getCreated());
	}

	@Test(expected = NotFoundException.class)
	public void testUpdateOpStateNoneFound() {
		controller.updateOpState("badid", OperatingState.disabled.toString());
	}

	@Test(expected = ServiceException.class)
	public void testUpdateOpStateException() throws Exception {
		unsetRepos();
		controller.updateOpState(id, OperatingState.disabled.toString());
	}

	@Test
	public void testUpdateOpStateByName() {
		assertTrue("Update did not complete successfully",
				controller.updateOpStateByName(TEST_NAME, OperatingState.disabled.toString()));
		DeviceManager d2 = repos.findByName(TEST_NAME);
		assertEquals("Update op state did not work correclty", OperatingState.disabled, d2.getOperatingState());
		assertNotNull("Modified date is null", d2.getModified());
		assertNotNull("Create date is null", d2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				d2.getModified() != d2.getCreated());
	}

	@Test(expected = NotFoundException.class)
	public void testUpdateOpStateByNameNoneFound() {
		controller.updateOpStateByName("badname", OperatingState.disabled.toString());
	}

	@Test(expected = ServiceException.class)
	public void testUpdateOpStateByNameException() throws Exception {
		unsetRepos();
		controller.updateOpStateByName(TEST_NAME, OperatingState.disabled.toString());
	}

	@Test
	public void testUpdateAdminState() {
		assertTrue("Update did not complete successfully",
				controller.updateAdminState(id, AdminState.locked.toString()));
		DeviceManager d2 = repos.findOne(id);
		assertEquals("Update admin state did not work correclty", AdminState.locked, d2.getAdminState());
		assertNotNull("Modified date is null", d2.getModified());
		assertNotNull("Create date is null", d2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				d2.getModified() != d2.getCreated());
	}

	@Test(expected = NotFoundException.class)
	public void testUpdateAdminStateNoneFound() {
		controller.updateAdminState("badid", AdminState.locked.toString());
	}

	@Test(expected = ServiceException.class)
	public void testUpdateAdminStateException() throws Exception {
		unsetRepos();
		controller.updateAdminState(id, AdminState.locked.toString());
	}

	@Test
	public void testUpdateAdminStateByName() {
		assertTrue("Update did not complete successfully",
				controller.updateAdminStateByName(TEST_NAME, AdminState.locked.toString()));
		DeviceManager d2 = repos.findByName(TEST_NAME);
		assertEquals("Update admin state did not work correclty", AdminState.locked, d2.getAdminState());
		assertNotNull("Modified date is null", d2.getModified());
		assertNotNull("Create date is null", d2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				d2.getModified() != d2.getCreated());
	}

	@Test(expected = NotFoundException.class)
	public void testUpdateAdminStateByNameNoneFound() {
		controller.updateOpStateByName("badname", AdminState.locked.toString());
	}

	@Test(expected = ServiceException.class)
	public void testUpdateAdminStateByNameException() throws Exception {
		unsetRepos();
		controller.updateOpStateByName(TEST_NAME, AdminState.locked.toString());
	}

	@Test(expected = DataValidationException.class)
	public void testUpdateAdminStateWithNullAdminState() {
		controller.updateAdminState(id, null);
	}

	@Test(expected = DataValidationException.class)
	public void testUpdateAdminStateByNameWithNullAdminState() {
		controller.updateAdminStateByName(TEST_NAME, null);
	}

	@Test(expected = DataValidationException.class)
	public void testUpdateOpStateWithNullOpState() {
		controller.updateOpState(id, null);
	}

	@Test(expected = DataValidationException.class)
	public void testUpdateOpStateByNameWithNullAdminState() {
		controller.updateOpStateByName(TEST_NAME, null);
	}

	@Test(expected = ServiceException.class)
	public void testUpdateException() throws Exception {
		unsetRepos();
		DeviceManager d = repos.findOne(id);
		d.setDescription("newdescription");
		controller.update(d);
	}

	@Test(expected = NotFoundException.class)
	public void testUpdateWithNone() {
		DeviceManager d = repos.findOne(id);
		d.setId("badid");
		d.setName("badname");
		d.setDescription("newdescription");
		controller.update(d);
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
