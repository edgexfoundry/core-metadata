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
package org.edgexfoundry.dao;

import java.util.List;

import org.edgexfoundry.domain.meta.Addressable;
import org.edgexfoundry.domain.meta.DeviceManager;
import org.edgexfoundry.domain.meta.DeviceProfile;
import org.edgexfoundry.domain.meta.DeviceService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface DeviceManagerRepository extends MongoRepository<DeviceManager, String> {

	DeviceManager findByName(String name);

	List<DeviceManager> findByLabelsIn(String label);

	Page<DeviceManager> findByLabels(String label, Pageable pageable);

	List<DeviceManager> findByService(DeviceService service);

	Page<DeviceManager> findByService(DeviceService service, Pageable pageable);

	List<DeviceManager> findByProfile(DeviceProfile profile);

	Page<DeviceManager> findByProfile(DeviceProfile profile, Pageable pageable);

	List<DeviceManager> findByServiceAndName(DeviceService service, String name);

	Page<DeviceManager> findByServiceAndName(DeviceService service, String name, Pageable pageable);

	List<DeviceManager> findByDeviceToo(boolean devideToo);

	Page<DeviceManager> findByDeviceToo(boolean devideToo, Pageable pageable);

	List<DeviceManager> findByAddressable(Addressable addressable);

	Page<DeviceManager> findByAddressable(Addressable addressable, Pageable pageable);

}
