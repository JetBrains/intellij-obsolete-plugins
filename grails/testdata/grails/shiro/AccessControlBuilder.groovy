package org.apache.shiro.grails

class AccessControlBuilder {
    private Class controllerClass
    private Map internalPermissionMap = [:]
    private Map internalRoleMap = [:]

    AccessControlBuilder(Class controllerClass) {
        this.controllerClass = controllerClass
    }

    def getPermissionMap() {
        return Collections.unmodifiableMap(this.internalPermissionMap)
    }

    def getRoleMap() {
        return Collections.unmodifiableMap(this.internalRoleMap)
    }

    def role(args) {
        def roleName = args['name']
        if (!roleName) {
            throw new RuntimeException('The [name] parameter is required when defining a role.')
        }

        if (args['action']) {
            // Single action requires this role.
            addRoleToAction(roleName, args['action'])
        }
        else if (args['only']) {
            // Several actions require this role.
            def actions = args['only']
            actions.each { action ->
                addRoleToAction(roleName, action)
            }
        }
        else {
            // All the actions require this role.
            addRoleToAction(roleName, '*')
        }
    }

    def permission(args) {
        def perm = args['perm']
        if (!perm) {
            throw new RuntimeException('The [perm] parameter is required when defining a permission.')
        }

        if (args['action']) {
            // Single action requires this permission.
            addPermissionToAction(perm, args['action'])
        }
        else if (args['only']) {
            // Several actions require this perm.
            def actions = args['only']
            actions.each { action ->
                addPermissionToAction(perm, action)
            }
        }
        else {
            // All the actions require this perm.
            addPermissionToAction(perm, '*')
        }
    }

    private addRoleToAction(role, action) {
        def roleList = internalRoleMap[action]
        if (!roleList) {
            internalRoleMap[action] = [ role ]
        }
        else {
            roleList << role
        }
    }

    private addPermissionToAction(permission, action) {
        def permissionList = internalPermissionMap[action]
        if (!permissionList) {
            internalPermissionMap[action] = [ permission ]
        }
        else {
            permissionList << permission
        }
    }
}
