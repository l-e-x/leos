/*
 * Copyright 2019 European Commission
 *
 * Licensed under the EUPL, Version 1.2 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.security;

import java.io.InputStream;
import java.util.*;


import eu.europa.ec.leos.permissions.Permissions;
import eu.europa.ec.leos.permissions.Role;
import eu.europa.ec.leos.permissions.Roles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xml.sax.SAXException;

import javax.annotation.PostConstruct;
import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

@Component
public class LeosPermissionAuthorityMap {

    private static final Logger LOG = LoggerFactory.getLogger(LeosPermissionAuthorityMap.class);

    private final Map<String, Set<LeosPermission>> permissionMap = new HashMap<>();

    private final Map<String, Role> roleMap = new HashMap<>();

    private final List<Role> listOfRoles = new ArrayList<>();

    @PostConstruct
    void createPermissionMap() {
        Roles roles;
        try {
            roles = unmarshallerRolesPermission();
        } catch (SAXException exception) {
            throw new RuntimeException("Invalid Roles Permission XML", exception);
        } catch (JAXBException exception) {
            throw new RuntimeException("Error occurred in role permission unmarshaller", exception);
        } catch (Exception exception) {
            throw new RuntimeException("Error occurred while creating leos Permission Hierarchy", exception);
        }
        if (roles != null) {
            listOfRoles.addAll(roles.getRoles());
            roles.getRoles().forEach(role -> roleMap.put(role.getName(), role));
            for (Role role : listOfRoles) {
                Permissions permissions = role.getPermissions();
                List<String> listOfPermissions = permissions != null ? permissions.getPermissions() : Collections.emptyList();
                Set<LeosPermission> rolePermissions = new HashSet<>();
                for (String permission : listOfPermissions) {
                    rolePermissions.add(getLeosPermission(permission));
                }
                permissionMap.put(role.getName(), rolePermissions);
            }
        }
    }

    private Roles unmarshallerRolesPermission() throws JAXBException, SAXException {
        JAXBContext jaxbContext = JAXBContext.newInstance(Roles.class);
        Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();

        SchemaFactory sf = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema rolePermSchema = sf.newSchema(new StreamSource(loadPermissionHierarchySchema()));
        jaxbUnmarshaller.setSchema(rolePermSchema);

        return (Roles) jaxbUnmarshaller.unmarshal(loadPermissionHierarchy());
    }

    private InputStream loadPermissionHierarchy() {
        return LeosPermissionAuthorityMap.class.getClassLoader().getResourceAsStream("leosPermissions.xml");
    }

    private InputStream loadPermissionHierarchySchema() {
        return LeosPermissionAuthorityMap.class.getClassLoader().getResourceAsStream("schema/leosPermissions.xsd");
    }

    private LeosPermission getLeosPermission(String permission) {
        try {
            return LeosPermission.valueOf(permission);
        } catch (IllegalArgumentException exception) {
            LOG.error("Illegal Argument in LeosPermission : " + permission, exception);
            return null;
        }
    }

    public Set<LeosPermission> getPermissions(String authority) {
        return permissionMap.get(authority);
    }
    
    public Set<LeosPermission> getPermissions(List<String> authorities) {
		Set<LeosPermission> permissionDetails = new HashSet<>();
		if (authorities != null) {
			for (String authority : authorities) {
				permissionDetails.addAll(permissionMap.get(authority));
			}
		}
		return permissionDetails;
	}

    public List<Role> getAllRoles(){
        return listOfRoles;
    }

    public Map<String, Role> getRoleMap() {
        return roleMap;
    }
}