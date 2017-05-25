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
import static org.edgexfoundry.test.data.DeviceData.newTestInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.edgexfoundry.Application;
import org.edgexfoundry.controller.DeviceController;
import org.edgexfoundry.dao.AddressableRepository;
import org.edgexfoundry.dao.DeviceProfileRepository;
import org.edgexfoundry.dao.DeviceRepository;
import org.edgexfoundry.dao.DeviceServiceRepository;
import org.edgexfoundry.domain.meta.Addressable;
import org.edgexfoundry.domain.meta.AdminState;
import org.edgexfoundry.domain.meta.Device;
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
public class DeviceControllerTest {

	private static final String LIMIT = "maxLimit";

	@Autowired
	DeviceRepository repos;

	@Autowired
	DeviceController controller;

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
		Device d = newTestInstance();
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
	public void testDevice() {
		Device d = controller.device(id);
		checkTestData(d, id);
	}

	@Test(expected = NotFoundException.class)
	public void testDeviceWithUnknownnId() {
		controller.device("nosuchid");
	}

	@Test(expected = ServiceException.class)
	public void testDeviceException() throws Exception {
		unsetRepos();
		controller.device(id);
	}

	@Test
	public void testDevices() {
		List<Device> as = controller.devices();
		assertEquals("Find all not returning a list with one device", 1, as.size());
		checkTestData(as.get(0), id);
	}

	@Test(expected = ServiceException.class)
	public void testDevicesException() throws Exception {
		unsetRepos();
		controller.devices();
	}

	@Test(expected = LimitExceededException.class)
	public void testDevicesMaxLimitExceeded() throws Exception {
		unsetControllerMAXLIMIT();
		controller.devices();
	}

	@Test
	public void testDeviceForName() {
		Device a = controller.deviceForName(TEST_NAME);
		checkTestData(a, id);
	}

	@Test(expected = NotFoundException.class)
	public void testDeviceForNameWithNoneMatching() {
		controller.deviceForName("badname");
	}

	@Test(expected = ServiceException.class)
	public void testDeviceForNameException() throws Exception {
		unsetRepos();
		controller.deviceForName(TEST_NAME);
	}

	@Test
	public void testDeviceByLabel() {
		List<Device> ds = controller.devicesByLabel(TEST_LABELS[0]);
		assertEquals("Find for labels not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test
	public void testDevicesByLabelWithNoneMatching() {
		assertTrue("No devices should be found with bad label", controller.devicesByLabel("badlabel").isEmpty());
	}

	@Test(expected = ServiceException.class)
	public void testDevicesByLabelException() throws Exception {
		unsetRepos();
		controller.devicesByLabel(TEST_LABELS[0]);
	}

	@Test
	public void testDevicesForAddressable() {
		List<Device> ds = controller.devicesForAddressable(addrId);
		assertEquals("Find for address not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test
	public void testDevicesForAddressableByName() {
		List<Device> ds = controller.devicesForAddressableByName(AddressableData.TEST_ADDR_NAME);
		assertEquals("Find for address not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test(expected = NotFoundException.class)
	public void testDevicesForAddressableWithNoneMatching() throws Exception {
		controller.devicesForAddressable("badaddress");
	}

	@Test(expected = NotFoundException.class)
	public void testDevicesForAddressableByNameWithNoneMatching() throws Exception {
		controller.devicesForAddressableByName("badaddress");
	}

	@Test(expected = ServiceException.class)
	public void testDevicesForAddressableException() throws Exception {
		unsetRepos();
		controller.devicesForAddressable(addrId);
	}

	@Test
	public void testDevicesForService() {
		List<Device> ds = controller.devicesForService(serviceId);
		assertEquals("Find for services not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test
	public void testDevicesForServiceByName() {
		List<Device> ds = controller.devicesForServiceByName(ServiceData.TEST_SERVICE_NAME);
		assertEquals("Find for services not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test(expected = NotFoundException.class)
	public void testDevicesForServiceWithNone() {
		controller.devicesForService("badservice");
	}

	@Test(expected = NotFoundException.class)
	public void testDevicesForServiceByNameWithNone() {
		controller.devicesForServiceByName("badservice");
	}

	@Test(expected = ServiceException.class)
	public void testDevicesForServiceException() throws Exception {
		unsetRepos();
		controller.devicesForService(serviceId);
	}

	@Test
	public void testDevicesForProfile() {
		List<Device> ds = controller.devicesForProfile(profileId);
		assertEquals("Find for profiles not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test
	public void testDevicesForProfileByName() {
		List<Device> ds = controller.devicesForProfileByName(ProfileData.TEST_PROFILE_NAME);
		assertEquals("Find for profiles not returning appropriate list", 1, ds.size());
		checkTestData(ds.get(0), id);
	}

	@Test(expected = NotFoundException.class)
	public void testDevicesForProfileWithNone() {
		assertTrue("No devices should be found with bad profile", controller.devicesForProfile("badprofile").isEmpty());
	}

	@Test(expected = NotFoundException.class)
	public void testDevicesForProfileByNameWithNone() {
		controller.devicesForProfileByName("badprofile");
	}

	@Test(expected = ServiceException.class)
	public void testDevicesForProfileException() throws Exception {
		unsetRepos();
		controller.devicesForProfile(profileId);
	}

	@Test
	public void testAdd() {
		Device d = repos.findOne(id);
		d.setId(null);
		d.setName("NewName");
		String newId = controller.add(d);
		assertNotNull("New device id is null", newId);
		assertNotNull("Modified date is null", d.getModified());
		assertNotNull("Create date is null", d.getCreated());
	}

	@Test(expected = DataValidationException.class)
	public void testAddWithNullAdminState() {
		Device d = repos.findOne(id);
		d.setId(null);
		d.setName("NewName");
		d.setAdminState(null);
		controller.add(d);
	}

	@Test(expected = DataValidationException.class)
	public void testAddWithNullOpState() {
		Device d = repos.findOne(id);
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
		Device d = repos.findOne(id);
		d.setId(null);
		controller.add(d);
	}

	@Test(expected = DataValidationException.class)
	public void testAddWithNoDeviceService() {
		Device d = repos.findOne(id);
		d.setId(null);
		d.setName("newname");
		d.setService(null);
		controller.add(d);
	}

	@Test(expected = DataValidationException.class)
	public void testAddWithNoDeviceProfile() {
		Device d = repos.findOne(id);
		d.setId(null);
		d.setName("newname");
		d.setProfile(null);
		controller.add(d);
	}

	@Test(expected = DataValidationException.class)
	public void testAddWithNoAddressable() {
		Device d = repos.findOne(id);
		d.setId(null);
		d.setName("newname");
		d.setAddressable(null);
		controller.add(d);
	}

	@Test(expected = ServiceException.class)
	public void testAddException() throws Exception {
		unsetRepos();
		Device d = repos.findOne(id);
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
		Device d = repos.findOne(id);
		d.setDescription("newdescription");
		assertTrue("Update did not complete successfully", controller.update(d));
		Device d2 = repos.findOne(id);
		assertEquals("Update did not work correclty", "newdescription", d2.getDescription());
		assertNotNull("Modified date is null", d2.getModified());
		assertNotNull("Create date is null", d2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				d2.getModified() != d2.getCreated());
	}

	@Test
	public void testUpdateLastConnected() {
		assertTrue("Update did not complete successfully", controller.updateLastConnected(id, 1000));
		Device d2 = repos.findOne(id);
		assertEquals("Update last connected did not work correclty", 1000, d2.getLastConnected());
		assertNotNull("Modified date is null", d2.getModified());
		assertNotNull("Create date is null", d2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				d2.getModified() != d2.getCreated());
	}

	@Test
	public void testUpdateLastConnectedAndNotify() {
		assertTrue("Update did not complete successfully", controller.updateLastConnected(id, 1000, true));
		Device d2 = repos.findOne(id);
		assertEquals("Update last connected with notify did not work correclty", 1000, d2.getLastConnected());
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
		Device d2 = repos.findByName(TEST_NAME);
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
		Device d2 = repos.findOne(id);
		assertEquals("Update last reported did not work correclty", 1000, d2.getLastReported());
		assertNotNull("Modified date is null", d2.getModified());
		assertNotNull("Create date is null", d2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				d2.getModified() != d2.getCreated());
	}

	@Test
	public void testUpdateLastReportedAndNotify() {
		assertTrue("Update did not complete successfully", controller.updateLastReported(id, 1000, true));
		Device d2 = repos.findOne(id);
		assertEquals("Update last reported and notify did not work correclty", 1000, d2.getLastReported());
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
		Device d2 = repos.findByName(TEST_NAME);
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
		Device d2 = repos.findOne(id);
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

	@Test
	public void testUpdateOpStateByName() {
		assertTrue("Update did not complete successfully",
				controller.updateOpStateByName(TEST_NAME, OperatingState.disabled.toString()));
		Device d2 = repos.findByName(TEST_NAME);
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
		Device d2 = repos.findOne(id);
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
		Device d2 = repos.findByName(TEST_NAME);
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

	@Test(expected = ServiceException.class)
	public void testUpdateException() throws Exception {
		unsetRepos();
		Device d = repos.findOne(id);
		d.setDescription("newdescription");
		controller.update(d);
	}

	@Test(expected = NotFoundException.class)
	public void testUpdateWithNone() {
		Device d = repos.findOne(id);
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
