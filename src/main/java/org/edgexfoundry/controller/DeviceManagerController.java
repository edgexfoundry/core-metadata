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
package org.edgexfoundry.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.edgexfoundry.dao.AddressableDao;
import org.edgexfoundry.dao.DeviceDao;
import org.edgexfoundry.dao.DeviceManagerDao;
import org.edgexfoundry.dao.DeviceManagerRepository;
import org.edgexfoundry.dao.DeviceProfileDao;
import org.edgexfoundry.dao.DeviceReportDao;
import org.edgexfoundry.dao.DeviceServiceDao;
import org.edgexfoundry.domain.meta.ActionType;
import org.edgexfoundry.domain.meta.Addressable;
import org.edgexfoundry.domain.meta.AdminState;
import org.edgexfoundry.domain.meta.Device;
import org.edgexfoundry.domain.meta.DeviceManager;
import org.edgexfoundry.domain.meta.DeviceProfile;
import org.edgexfoundry.domain.meta.DeviceService;
import org.edgexfoundry.domain.meta.OperatingState;
import org.edgexfoundry.exception.controller.DataValidationException;
import org.edgexfoundry.exception.controller.LimitExceededException;
import org.edgexfoundry.exception.controller.NotFoundException;
import org.edgexfoundry.exception.controller.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * DeviceManagers are aggregators of other devices that may also be devices
 * themselves (e.g. as in Zigbee). This controller behaves a lot like (and
 * reuses code from) the Device Controller on purpose.
 * 
 */
@RestController
@RequestMapping("/api/v1/devicemanager")
public class DeviceManagerController {

	@Value("${read.max.limit}")
	private int maxLimit;

	// private static final Logger logger =
	// Logger.getLogger(DeviceManagerController.class);
	private final static org.edgexfoundry.support.logging.client.EdgeXLogger logger = org.edgexfoundry.support.logging.client.EdgeXLoggerFactory
			.getEdgeXLogger(DeviceManagerController.class);

	@Autowired
	private DeviceManagerRepository repos;

	@Autowired
	private AddressableDao addressableDao;

	@Autowired
	private DeviceProfileDao profileDao;

	@Autowired
	private DeviceServiceDao serviceDao;

	@Autowired
	private DeviceManagerDao dao;

	@Autowired
	private DeviceDao deviceDao;

	@Autowired
	private DeviceReportDao deviceRptDao;

	@Autowired
	private CallbackExecutor callback;

	/**
	 * Fetch a specific device manager by database generated id. May return null
	 * if no manager with the id is found. Returns ServiceException (HTTP 503)
	 * for unknown or unanticipated issues. Returns NotFoundException (HTTP 404)
	 * if no DeviceManager matches on the id provided.
	 * 
	 * @param String
	 *            database generated id for the manager
	 * 
	 * @return manager matching on the id
	 */
	@RequestMapping(value = "/{id}", method = RequestMethod.GET)
	public DeviceManager deviceManager(@PathVariable String id) {
		try {
			DeviceManager m = repos.findOne(id);
			if (m == null)
				throw new NotFoundException(DeviceManager.class.toString(), id);
			return m;
		} catch (NotFoundException nfE) {
			throw nfE;
		} catch (Exception e) {
			logger.error("Error getting device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Return all managers. Returns ServiceException (HTTP 503) for unknown or
	 * unanticipated issues. Returns LimitExceededException (HTTP 413) if the
	 * number returned exceeds the max limit.
	 * 
	 * @return list of device managers
	 */
	@RequestMapping(method = RequestMethod.GET)
	public List<DeviceManager> deviceManagers() {
		try {
			if (repos.count() > maxLimit) {
				logger.error("Max limit exceeded on request for device managers");
				throw new LimitExceededException("DeviceManager");
			}
			Sort sort = new Sort(Sort.Direction.DESC, "_id");
			return repos.findAll(sort);
		} catch (LimitExceededException lE) {
			throw lE;
		} catch (Exception e) {
			logger.error("Error getting device managers:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Return DeviceManagers matching given name (manager names should be
	 * unique). May be null if no manager matches on the name provided. Returns
	 * ServiceException (HTTP 503) for unknown or unanticipated issues. Returns
	 * NotFoundException (HTTP 404) if no DeviceManager matches on the id
	 * provided.
	 * 
	 * @param name
	 * @return device manager matching on name
	 */
	@RequestMapping(value = "/name/{name:.+}", method = RequestMethod.GET)
	public DeviceManager deviceManagerForName(@PathVariable String name) {
		try {
			DeviceManager m = repos.findByName(name);
			if (m == null)
				throw new NotFoundException(DeviceManager.class.toString(), name);
			return m;
		} catch (NotFoundException nfE) {
			throw nfE;
		} catch (Exception e) {
			logger.error("Error getting device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Find all DeviceManagers having at least one label matching the label
	 * provided. List may be empty if no managers match. Returns
	 * ServiceException (HTTP 503) for unknown or unanticipated issues.
	 * 
	 * @param label
	 *            - label to be matched
	 * 
	 * @return List of DeviceManager matching on specified label
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/label/{label:.+}")
	public List<DeviceManager> deviceManagerByLabel(@PathVariable String label) {
		try {
			return repos.findByLabelsIn(label);
		} catch (Exception e) {
			logger.error("Error getting device managers:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Find all managers associated to the DeviceService with the specified
	 * DeviceService database generated identifier. List may be empty if no
	 * managers match. Returns ServiceException (HTTP 503) for unknown or
	 * unanticipated issues. Returns NotFoundException (HTTP 404) if no
	 * DeviceManager match on the id provided.
	 * 
	 * @param serviceId
	 *            - device service's database generated identifier
	 * @return List of DeviceManagers associated to the device service
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/service/{serviceId}")
	public List<DeviceManager> deviceManagersForService(@PathVariable String serviceId) {
		try {
			DeviceService service = serviceDao.getById(serviceId);
			if (service == null) {
				logger.error("Request for device managers by non-existent service:  " + serviceId);
				throw new NotFoundException(DeviceService.class.toString(), serviceId);
			}
			return repos.findByService(service);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error getting device managers:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Find all managers associated to the DeviceService with the specified
	 * service name (DeviceService names must be unique). List may be empty if
	 * no managers match. Returns ServiceException (HTTP 503) for unknown or
	 * unanticipated issues. Returns NotFoundException (HTTP 404) if no
	 * DeviceService match on the name provided.
	 * 
	 * @param servicename
	 *            - device service's name
	 * @return List of DeviceManagers associated to the device service
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/servicename/{servicename:.+}")
	public List<DeviceManager> deviceManagersForServiceByName(@PathVariable String servicename) {
		try {
			DeviceService service = serviceDao.getByName(servicename);
			if (service == null) {
				logger.error("Request for device manager by non-existent service name:  " + servicename);
				throw new NotFoundException(DeviceService.class.toString(), servicename);
			}
			return repos.findByService(service);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error getting device managers:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Find all managers associated to the DeviceProfile with the specified
	 * profile database generated identifier. List may be empty if no managers
	 * match. Returns ServiceException (HTTP 503) for unknown or unanticipated
	 * issues. Returns NotFoundException (HTTP 404) if no DeviceProfile match on
	 * the id provided.
	 * 
	 * @param profile
	 *            id - device profile's database generated identifier
	 * @return List of DeviceManagers associated to the device profile
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/profile/{profileId}")
	public List<DeviceManager> deviceManagersForProfile(@PathVariable String profileId) {
		try {
			DeviceProfile profile = profileDao.getById(profileId);
			if (profile == null) {
				logger.error("Request for device managers by non-existent profile:  " + profileId);
				throw new NotFoundException(DeviceProfile.class.toString(), profileId);
			}
			return repos.findByProfile(profile);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error getting device managers:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Find all managers associated to the DeviceProfile with the specified
	 * profile name. List may be empty if no managers match. Returns
	 * ServiceException (HTTP 503) for unknown or unanticipated issues. Returns
	 * NotFoundException (HTTP 404) if no DeviceProfile match on the name
	 * provided.
	 * 
	 * @param profile
	 *            name - device profile's name
	 * @return List of DeviceManagers associated to the device profile
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/profilename/{profilename:.+}")
	public List<DeviceManager> deviceManagersForProfileByName(@PathVariable String profilename) {
		try {
			DeviceProfile profile = profileDao.getByName(profilename);
			if (profile == null) {
				logger.error("Request for device managers by non-existent profile:  " + profilename);
				throw new NotFoundException(DeviceProfile.class.toString(), profilename);
			}
			return repos.findByProfile(profile);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error getting device managers:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Find all managers associated to the Addressable with the specified
	 * addressable database generated identifier. List may be empty if no
	 * managers match. Returns ServiceException (HTTP 503) for unknown or
	 * unanticipated issues. Returns NotFoundException (HTTP 404) if no
	 * Addressable match on the id provided.
	 * 
	 * @param addressable
	 *            id - addressable's database generated identifier
	 * @return List of DeviceManagers associated to the addressable
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/addressable/{addressableId}")
	public List<DeviceManager> deviceManagersForAddressable(@PathVariable String addressableId) {
		try {
			Addressable addressable = addressableDao.getById(addressableId);
			if (addressable == null) {
				logger.error("Request for device manager by non-existent addressable:  " + addressableId);
				throw new NotFoundException(Addressable.class.toString(), addressableId);
			}
			return repos.findByAddressable(addressable);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error getting device managers:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Find all managers associated to the Addressable with the specified
	 * addressable name. List may be empty if no managers match. Returns
	 * ServiceException (HTTP 503) for unknown or unanticipated issues. Returns
	 * NotFoundException (HTTP 404) if no Addressable match on the name
	 * provided.
	 * 
	 * @param addressable
	 *            name - addressable's name
	 * @return List of DeviceManagers associated to the addressable
	 */
	@RequestMapping(method = RequestMethod.GET, value = "/addressablename/{addressablename:.+}")
	public List<DeviceManager> deviceManagersForAddressableByName(@PathVariable String addressablename) {
		try {
			Addressable addressable = addressableDao.getByName(addressablename);
			if (addressable == null) {
				logger.error("Request for device manager by non-existent addressable:  " + addressablename);
				throw new NotFoundException(Addressable.class.toString(), addressablename);
			}
			return repos.findByAddressable(addressable);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error getting device managers:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Add a new DeviceManager - name must be unique. Embedded objects (device,
	 * service, profile, addressable) are all referenced in the new
	 * DeviceManager object by id or name to associated objects. All other data
	 * in the embedded objects will be ignored. Returns ServiceException (HTTP
	 * 503) for unknown or unanticipated issues. Returns DataValidationException
	 * (HTTP 409) if an associated object (Addressable, Profile, Service) cannot
	 * be found with the id or name provided.
	 * 
	 * @param DeviceManager
	 *            object
	 * @return database generated identifier for the new device manager
	 */
	@RequestMapping(method = RequestMethod.POST)
	public String add(@RequestBody DeviceManager manager) {
		if (manager == null)
			throw new ServiceException(new DataValidationException("No device manager data provided"));
		if (manager.getAdminState() == null || manager.getOperatingState() == null)
			throw new DataValidationException("Device Manager and Admin state cannot be null");
		try {
			attachAssociated(manager);
			repos.save(manager);
			notifyAssociates(manager, Action.POST);
			return manager.getId();
		} catch (DuplicateKeyException dE) {
			throw new DataValidationException("Name is not unique: " + manager.getName());
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error adding device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	private void attachAssociated(DeviceManager manager) {
		DeviceService service = serviceDao.getByIdOrName(manager.getService());
		if (service == null)
			throw new DataValidationException("A device must be associated to a known device service.");
		manager.setService(service);
		DeviceProfile profile = profileDao.getByIdOrName(manager.getProfile());
		if (profile == null)
			throw new DataValidationException("A device must be associated to a known device profile.");
		manager.setProfile(profile);
		Addressable addressable = addressableDao.getByIdOrName(manager.getAddressable());
		if (addressable == null)
			throw new DataValidationException("A device must be associated to a known addressable.");
		manager.setAddressable(addressable);

		manager.setDevices(fetchAssociatedDevices(manager));
		manager.setManagers(fetchAssociatedDeviceManagers(manager));
	}

	private List<Device> fetchAssociatedDevices(DeviceManager manager) {
		if (manager.getDevices() != null) {
			return manager.getDevices().stream().map(d -> deviceDao.getByIdOrName(d)).collect(Collectors.toList());
		}
		return null;
	}

	private List<DeviceManager> fetchAssociatedDeviceManagers(DeviceManager manager) {
		if (manager.getManagers() != null) {
			return manager.getManagers().stream().map(d -> dao.getByIdOrName(d)).collect(Collectors.toList());
		}
		return null;
	}

	/**
	 * Update the last connected time of the manager by database generated
	 * identifier. Returns ServiceException (HTTP 503) for unknown or
	 * unanticipated issues. Returns NotFoundException (HTTP 404) if the device
	 * cannot be found by the identifier provided.
	 * 
	 * @param id
	 *            - database generated identifier for the manager
	 * @param time
	 *            - new last connected time in milliseconds
	 * @return boolean indicating success of update
	 */
	@RequestMapping(value = "/{id}/lastconnected/{time}", method = RequestMethod.PUT)
	public boolean updateLastConnected(@PathVariable String id, @PathVariable long time) {
		try {
			DeviceManager manager = repos.findOne(id);
			if (manager == null) {
				logger.error("Request to update last connected time with non-existent device manager:  " + id);
				throw new NotFoundException(DeviceManager.class.toString(), id);
			}
			return updateLastConnected(manager, time);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error updating device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Update the last connected time of the manager by unique name of the
	 * manager. Returns ServiceException (HTTP 503) for unknown or unanticipated
	 * issues. Returns NotFoundException (HTTP 404) if the device cannot be
	 * found by the name provided.
	 * 
	 * @param name
	 *            - manager name
	 * @param time
	 *            - new last connected time in milliseconds
	 * @return boolean indicating success of update
	 */
	@RequestMapping(value = "/name/{name:.+}/lastconnected/{time}", method = RequestMethod.PUT)
	public boolean updateLastConnectedByName(@PathVariable String name, @PathVariable long time) {
		try {
			DeviceManager manager = repos.findByName(name);
			if (manager == null) {
				logger.error("Request to update last connected time with non-existent device manager:  " + name);
				throw new NotFoundException(DeviceManager.class.toString(), name);
			}
			return updateLastConnected(manager, time);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error updating device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	private boolean updateLastConnected(DeviceManager manager, long time) {
		try {
			manager.setLastConnected(time);
			repos.save(manager);
			notifyAssociates(manager, Action.PUT);
			return true;
		} catch (Exception e) {
			logger.error("Error updating last connected time for the device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Update the last reported time of the manager by database generated
	 * identifier. Returns ServiceException (HTTP 503) for unknown or
	 * unanticipated issues. Returns NotFoundException (HTTP 404) if the manager
	 * cannot be found by the identifier provided.
	 * 
	 * @param id
	 *            - database generated identifier for the manager
	 * @param time
	 *            - new last reported time in milliseconds
	 * @return boolean indicating success of update
	 */
	@RequestMapping(value = "/{id}/lastreported/{time}", method = RequestMethod.PUT)
	public boolean updateLastReported(@PathVariable String id, @PathVariable long time) {
		try {
			DeviceManager manager = repos.findOne(id);
			if (manager == null) {
				logger.error("Request to update last reported time with non-existent device manager:  " + id);
				throw new NotFoundException(DeviceManager.class.toString(), id);
			}
			return updateLastReported(manager, time);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error updating device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Update the last reported time of the device by unique name of the
	 * manager. Returns ServiceException (HTTP 503) for unknown or unanticipated
	 * issues. Returns NotFoundException (HTTP 404) if the manager cannot be
	 * found by the name provided.
	 * 
	 * @param name
	 *            - manager name
	 * @param time
	 *            - new last reported time in milliseconds
	 * @return boolean indicating success of update
	 */
	@RequestMapping(value = "/name/{name:.+}/lastreported/{time}", method = RequestMethod.PUT)
	public boolean updateLastReportedByName(@PathVariable String name, @PathVariable long time) {
		try {
			DeviceManager manager = repos.findByName(name);
			if (manager == null) {
				logger.error("Request to update last reported time with non-existent device manager:  " + name);
				throw new NotFoundException(DeviceManager.class.toString(), name);
			}
			return updateLastReported(manager, time);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error updating device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	private boolean updateLastReported(DeviceManager manager, long time) {
		try {
			manager.setLastReported(time);
			repos.save(manager);
			notifyAssociates(manager, Action.PUT);
			return true;
		} catch (Exception e) {
			logger.error("Error updating last reported time for the device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Update the op state of the manager by database generated identifier.
	 * Returns ServiceException (HTTP 503) for unknown or unanticipated issues.
	 * Returns NotFoundException (HTTP 404) if the device cannot be found by the
	 * identifier provided. Returns DataValidationException (HTTP 409) if the
	 * proposed new state is null;
	 * 
	 * @param id
	 *            - database generated identifier for the manager
	 * @param opState
	 *            - new op state for the manager (either enabled or disabled)
	 * @return - boolean indicating success of the operation
	 */
	@RequestMapping(value = "/{id}/opstate/{opState}", method = RequestMethod.PUT)
	public boolean updateOpState(@PathVariable String id, @PathVariable String opState) {
		try {
			DeviceManager manager = repos.findOne(id);
			if (manager == null) {
				logger.error("Request to update op state with non-existent device manager:  " + id);
				throw new NotFoundException(DeviceManager.class.toString(), id);
			}
			return updateOpState(manager, opState);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error updating device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Update the op status time of the manager by unique name of the manager.
	 * Returns ServiceException (HTTP 503) for unknown or unanticipated issues.
	 * Returns NotFoundException (HTTP 404) if the manager cannot be found by
	 * the name provided. Returns DataValidationException (HTTP 409) if the
	 * proposed new state is null;
	 * 
	 * @param name
	 *            - manager name
	 * @param opState
	 *            - new op state for the manager (either enabled or disabled)
	 * @return - boolean indicating success of the operation
	 */
	@RequestMapping(value = "/name/{name:.+}/opstate/{opState}", method = RequestMethod.PUT)
	public boolean updateOpStateByName(@PathVariable String name, @PathVariable String opState) {
		try {
			DeviceManager manager = repos.findByName(name);
			if (manager == null) {
				logger.error("Request to update op state with non-existent device manager name:  " + name);
				throw new NotFoundException(DeviceManager.class.toString(), name);
			}
			return updateOpState(manager, opState);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error updating device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	private boolean updateOpState(DeviceManager manager, String state) {
		if (state == null)
			throw new DataValidationException("Op state cannot be set to null");
		try {
			manager.setOperatingState(OperatingState.valueOf(state));
			repos.save(manager);
			notifyAssociates(manager, Action.PUT);
			return true;
		} catch (Exception e) {
			logger.error("Error updating op state for the device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Update the admin state of the manager by database generated identifier.
	 * Returns ServiceException (HTTP 503) for unknown or unanticipated issues.
	 * Returns NotFoundException (HTTP 404) if the manager cannot be found by
	 * the identifier provided. Returns DataValidationException (HTTP 409) if
	 * the proposed new state is null;
	 * 
	 * @param id
	 *            - database generated identifier for the manager
	 * @param adminstate
	 *            - new admin state for the manager (either locked or unlocked)
	 * @return - boolean indicating success of the operation
	 */
	@RequestMapping(value = "/{id}/adminstate/{adminState}", method = RequestMethod.PUT)
	public boolean updateAdminState(@PathVariable String id, @PathVariable String adminState) {
		try {
			DeviceManager manager = repos.findOne(id);
			if (manager == null) {
				logger.error("Request to update admin state with non-existent device manager:  " + id);
				throw new NotFoundException(DeviceManager.class.toString(), id);
			}
			boolean success = updateAdminState(manager, adminState);
			notifyAssociates(manager, Action.PUT);
			return success;
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error updating device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Update the admin state of the manager by manager name. Returns
	 * ServiceException (HTTP 503) for unknown or unanticipated issues. Returns
	 * NotFoundException (HTTP 404) if the manager cannot be found by the
	 * identifier provided. Returns DataValidationException (HTTP 409) if the
	 * proposed new state is null;
	 * 
	 * @param name
	 *            - manager name
	 * @param opState
	 *            - new admin state for the manager (either locked or unlocked)
	 * @return - boolean indicating success of the operation
	 */
	@RequestMapping(value = "/name/{name:.+}/adminstate/{adminState}", method = RequestMethod.PUT)
	public boolean updateAdminStateByName(@PathVariable String name, @PathVariable String adminState) {
		try {
			DeviceManager manager = repos.findByName(name);
			if (manager == null) {
				logger.error("Request to update admin state with non-existent device manager name:  " + name);
				throw new NotFoundException(DeviceManager.class.toString(), name);
			}
			boolean success = updateAdminState(manager, adminState);
			notifyAssociates(manager, Action.PUT);
			return success;
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error updating device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	private boolean updateAdminState(DeviceManager manager, String state) {
		if (state == null)
			throw new DataValidationException("Admin state cannot be set to null");
		try {
			manager.setAdminState(AdminState.valueOf(state));
			repos.save(manager);
			return true;
		} catch (Exception e) {
			logger.error("Error updating admin state for the device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Update the Device Manager identified by the id or name stored in the
	 * object provided. Id is used first, name is used second for identification
	 * purposes. New device services & profiles cannot be created with a PUT,
	 * but the service and profile can replaced by referring to a new device
	 * service or profile id or name. Returns ServiceException (HTTP 503) for
	 * unknown or unanticipated issues. Returns NotFoundException (HTTP 404) if
	 * the device manager cannot be found by the identifier provided
	 * 
	 * @param manager2
	 *            - object holding the identifier and new values for the manager
	 * @return boolean indicating success of the update
	 */
	@RequestMapping(method = RequestMethod.PUT)
	public boolean update(@RequestBody DeviceManager manager2) {
		try {
			DeviceManager manager = dao.getByIdOrName(manager2);
			if (manager == null) {
				logger.error("Request to update with non-existent or unidentified device manager (id/name):  "
						+ manager2.getId() + "/" + manager2.getName());
				throw new NotFoundException(DeviceManager.class.toString(), manager2.getId());
			}
			updateDeviceManager(manager2, manager);
			notifyAssociates(manager, Action.PUT);
			return true;
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error updating device:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	private void updateDeviceManager(DeviceManager from, DeviceManager to) {
		if (from.getAddressable() != null)
			to.setAddressable(addressableDao.getByIdOrName(from.getAddressable()));
		if (from.getDevices() != null)
			to.setDevices(fetchAssociatedDevices(from));
		if (from.getManagers() != null)
			to.setManagers(fetchAssociatedDeviceManagers(from));
		if (from.getService() != null)
			to.setService(serviceDao.getByIdOrName(from.getService()));
		if (from.getProfile() != null)
			to.setProfile(profileDao.getByIdOrName(from.getProfile()));
		if (from.getAdminState() != null)
			to.setAdminState(from.getAdminState());
		if (from.getDescription() != null)
			to.setDescription(from.getDescription());
		if (from.getLabels() != null)
			to.setLabels(from.getLabels());
		if (from.getLastConnected() != 0)
			to.setLastConnected(from.getLastConnected());
		if (from.getLastReported() != 0)
			to.setLastReported(from.getLastReported());
		if (from.getLocation() != null)
			to.setLocation(from.getLocation());
		if (from.getOperatingState() != null)
			to.setOperatingState(from.getOperatingState());
		if (from.getOrigin() != 0)
			to.setOrigin(from.getOrigin());
		if (from.getName() != null)
			to.setName(from.getName());
		repos.save(to);
	}

	/**
	 * Remove the DeviceManager designated by database generated id. This does
	 * not remove associated objects (addressable, service, profile, etc.).
	 * Returns ServiceException (HTTP 503) for unknown or unanticipated issues.
	 * Returns NotFoundException (HTTP 404) if the manager cannot be found by
	 * the identifier provided.
	 * 
	 * @param database
	 *            generated id for the manager
	 * @return boolean indicating success of the remove operation
	 */
	@RequestMapping(value = "/id/{id}", method = RequestMethod.DELETE)
	public boolean delete(@PathVariable String id) {
		try {
			DeviceManager manager = repos.findOne(id);
			if (manager == null) {
				logger.error("Request to delete with non-existent device manager by id:  " + id);
				throw new NotFoundException(DeviceManager.class.toString(), id);
			}
			return deleteDeviceManager(manager);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error removing device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	/**
	 * Remove the DeviceManager designated by unique name. This does not remove
	 * associated objects (addressable, service, profile, etc.). Returns
	 * ServiceException (HTTP 503) for unknown or unanticipated issues. Returns
	 * NotFoundException (HTTP 404) if the manager cannot be found by the
	 * identifier provided.
	 * 
	 * @param unique
	 *            name of the manager
	 * @return boolean indicating success of the remove operation
	 */
	@RequestMapping(value = "/name/{name:.+}", method = RequestMethod.DELETE)
	public boolean deleteByName(@PathVariable String name) {
		try {
			DeviceManager manager = repos.findByName(name);
			if (manager == null) {
				logger.error("Request to delete with unknown device manager by name:  " + name);
				throw new NotFoundException(DeviceManager.class.toString(), name);
			}
			return deleteDeviceManager(manager);
		} catch (NotFoundException nE) {
			throw nE;
		} catch (DataValidationException dE) {
			throw dE;
		} catch (Exception e) {
			logger.error("Error removing device manager:  " + e.getMessage());
			throw new ServiceException(e);
		}
	}

	private boolean deleteDeviceManager(DeviceManager manager) {
		deviceRptDao.removeAssociatedReportsForDevice(manager);
		repos.delete(manager);
		notifyAssociates(manager, Action.DELETE);
		return true;
	}

	private void notifyAssociates(DeviceManager device, Action action) {
		callback.callback(device.getService(), device.getId(), action, ActionType.MANAGER);
	}
}
