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

import static org.edgexfoundry.test.data.ScheduleEventData.TEST_SCHEDULE_EVENT_NAME;
import static org.edgexfoundry.test.data.ScheduleEventData.checkTestData;
import static org.edgexfoundry.test.data.ScheduleEventData.newTestInstance;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;
import java.util.List;

import org.edgexfoundry.Application;
import org.edgexfoundry.controller.ScheduleEventController;
import org.edgexfoundry.dao.AddressableRepository;
import org.edgexfoundry.dao.DeviceReportRepository;
import org.edgexfoundry.dao.ScheduleEventRepository;
import org.edgexfoundry.dao.ScheduleRepository;
import org.edgexfoundry.domain.meta.Addressable;
import org.edgexfoundry.domain.meta.DeviceReport;
import org.edgexfoundry.domain.meta.Schedule;
import org.edgexfoundry.domain.meta.ScheduleEvent;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.exception.controller.ServiceException;
import org.edgexfoundry.test.category.RequiresMongoDB;
import org.edgexfoundry.test.category.RequiresSpring;
import org.edgexfoundry.test.category.RequiresWeb;
import org.edgexfoundry.test.data.AddressableData;
import org.edgexfoundry.test.data.ReportData;
import org.edgexfoundry.test.data.ScheduleData;
import org.edgexfoundry.test.data.ScheduleEventData;
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
public class ScheduleEventControllerTest {

	private static final String LIMIT = "maxLimit";

	@Autowired
	private ScheduleEventRepository repos;

	@Autowired
	private AddressableRepository addrRepos;

	@Autowired
	private ScheduleRepository scheduleRepos;

	@Autowired
	private DeviceReportRepository rptRepos;

	@Autowired
	private ScheduleEventController controller;

	private String id;

	@Before
	public void setup() {
		Schedule schedule = ScheduleData.newTestInstance();
		scheduleRepos.save(schedule);
		ScheduleEvent s = ScheduleEventData.newTestInstance();
		Addressable a = AddressableData.newTestInstance();
		addrRepos.save(a);
		s.setAddressable(a);
		repos.save(s);
		id = s.getId();
	}

	@After
	public void cleanup() throws Exception {
		resetControllerMAXLIMIT();
		resetRepos();
		scheduleRepos.deleteAll();
		rptRepos.deleteAll();
		addrRepos.deleteAll();
		repos.deleteAll();
	}

	@Test
	public void testScheduleEvent() {
		ScheduleEvent d = controller.scheduleEvent(id);
		checkTestData(d, id);
	}

	@Test(expected = NotFoundException.class)
	public void testScheduleEventWithUnknownId() {
		controller.scheduleEvent("nosuchid");
	}

	@Test(expected = ServiceException.class)
	public void testScheduleEventException() throws Exception {
		unsetRepos();
		controller.scheduleEvent(id);
	}

	@Test
	public void testScheduleEvents() {
		List<ScheduleEvent> as = controller.scheduleEvents();
		assertEquals("Find all not returning a list with one schedule event", 1, as.size());
		checkTestData(as.get(0), id);
	}

	@Test(expected = ServiceException.class)
	public void testScheduleEventsException() throws Exception {
		unsetRepos();
		controller.scheduleEvents();
	}

	@Test(expected = LimitExceededException.class)
	public void testScheduleEventsMaxLimitExceeded() throws Exception {
		unsetControllerMAXLIMIT();
		controller.scheduleEvents();
	}

	@Test
	public void testScheduleEventForName() {
		ScheduleEvent d = controller.scheduleEventForName(TEST_SCHEDULE_EVENT_NAME);
		checkTestData(d, id);
	}

	@Test(expected = NotFoundException.class)
	public void testScheduleEventForNameWithNoneMatching() {
		controller.scheduleEventForName("badname");
	}

	@Test(expected = ServiceException.class)
	public void testScheduleEventForNameException() throws Exception {
		unsetRepos();
		controller.scheduleEventForName(TEST_SCHEDULE_EVENT_NAME);
	}

	@Test
	public void testAdd() {
		ScheduleEvent d = newTestInstance();
		d.setName("NewName");
		String newId = controller.add(d);
		assertNotNull("New device id is null", newId);
		assertNotNull("Modified date is null", d.getModified());
		assertNotNull("Create date is null", d.getCreated());
	}

	@Test(expected = NotFoundException.class)
	public void testAddWithBadScheduleName() {
		ScheduleEvent d = newTestInstance();
		d.setName("NewName");
		d.setSchedule("badschedule");
		controller.add(d);
	}

	@Test(expected = ServiceException.class)
	public void testAddWithNoScheduleName() {
		ScheduleEvent d = newTestInstance();
		d.setName("NewName");
		d.setSchedule(null);
		controller.add(d);
	}

	@Test(expected = ServiceException.class)
	public void testAddNull() {
		controller.add(null);
	}

	@Test(expected = DataValidationException.class)
	public void testAddWithSameName() {
		ScheduleEvent d = repos.findOne(id);
		d.setId(null);
		controller.add(d);
	}

	@Test(expected = ServiceException.class)
	public void testAddException() throws Exception {
		unsetRepos();
		ScheduleEvent d = newTestInstance();
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
		assertTrue("Delete did not return correctly", controller.deleteByName(TEST_SCHEDULE_EVENT_NAME));
	}

	@Test(expected = NotFoundException.class)
	public void testDeleteByNameWithNone() {
		controller.delete("badname");
	}

	@Test(expected = ServiceException.class)
	public void testDeleteByNameException() throws Exception {
		unsetRepos();
		controller.deleteByName(TEST_SCHEDULE_EVENT_NAME);
	}

	@Test(expected = DataValidationException.class)
	public void testDeleteAssociatedToDeviceReport() {
		DeviceReport report = ReportData.newTestInstance();
		rptRepos.save(report);
		controller.delete(id);
	}

	@Test
	public void testUpdate() {
		ScheduleEvent d = repos.findOne(id);
		d.setOrigin(1234);
		assertTrue("Update did not complete successfully", controller.update(d));
		ScheduleEvent d2 = repos.findOne(id);
		assertEquals("Update did not work correclty", 1234, d2.getOrigin());
		assertNotNull("Modified date is null", d2.getModified());
		assertNotNull("Create date is null", d2.getCreated());
		assertTrue("Modified date and create date should be different after update",
				d2.getModified() != d2.getCreated());
	}

	@Test(expected = NotFoundException.class)
	public void testUpdateWithBadScheduleName() {
		ScheduleEvent d = repos.findOne(id);
		d.setSchedule("badschedule");
		controller.update(d);
	}

	@Test
	public void testUpdateChangeNameWhileNotAssocToDeviceReport() {
		ScheduleEvent d = repos.findOne(id);
		d.setName("newname");
		assertTrue("Update did not complete successfully", controller.update(d));
	}

	@Test(expected = DataValidationException.class)
	public void testUpdateChangeNameWhileAssocToDeviceReport() {
		DeviceReport report = ReportData.newTestInstance();
		rptRepos.save(report);
		ScheduleEvent d = repos.findOne(id);
		d.setName("newname");
		controller.update(d);
	}

	@Test(expected = ServiceException.class)
	public void testUpdateException() throws Exception {
		unsetRepos();
		ScheduleEvent d = repos.findOne(id);
		d.setOrigin(1234);
		controller.update(d);
	}

	@Test(expected = NotFoundException.class)
	public void testUpdateWithNone() {
		ScheduleEvent d = repos.findOne(id);
		d.setId("badid");
		d.setName("badname");
		d.setOrigin(1234);
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
