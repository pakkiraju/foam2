/**
 * @license
 * Copyright 2017 The FOAM Authors. All Rights Reserved.
 * http://www.apache.org/licenses/LICENSE-2.0
 */

foam.CLASS({
  package: 'foam.nanos.auth',
  name: 'Group',

  implements: [ 'foam.nanos.auth.EnabledAware' ],

  requires: [ 'foam.nanos.app.AppConfig' ],

  documentation: 'A Group of Users.',

  tableColumns: [ 'id', 'description', 'defaultMenu', 'parent' ],

  searchColumns: [ ],

  properties: [
    {
      class: 'String',
      name: 'id',
      documentation: 'Unique name of the Group.'
    },
    {
      class: 'String',
      name: 'description',
      documentation: 'Description of the Group.'
    },
    {
      class: 'String',
      name: 'parent',
      documentation: 'Parent group to inherit permissions from.'
    },
    {
      class: 'FObjectArray',
      of: 'foam.nanos.auth.Permission',
      name: 'permissions'
    },
    {
      class: 'Reference',
      targetDAOKey: 'menuDAO',
      name: 'defaultMenu',
      of: 'foam.nanos.menu.Menu'
    },
    { class: 'URL', name: 'logo' },
/*    {
      class: 'FObjectProperty',
      of: 'foam.nanos.app.AppConfig',
      name: 'appConfig',
      factory: function() { return this.AppConfig.create(); },
      documentation: 'Custom application configuration for group.'
    }
*/
    /*
      FUTURE
    {
      class: 'FObjectProperty',
      of: 'AuthConfig',
      documentation: 'Custom authentication settings for this group.'
    }
    */
  ],

  methods: [
    {
      name: 'implies',
      javaReturns: 'Boolean',
      args: [
        {
          name: 'permission',
          javaType: 'java.security.Permission'
        }
      ],
      javaCode: `
        if ( getPermissions() == null ) return false;
        for ( int i = 0 ; i < permissions_.length ; i++ ) {
          if ( new javax.security.auth.AuthPermission(permissions_[i].getId()).implies(permission) ) {
            return true;
          }
        }
        return false;`
      ,
      code: function(permissionId) {
        if ( this.permissions == null ) return false;

        for ( var i = 0 ; i < this.permissions.length ; i++ )
          if ( this.permissions[i].implies(permissionId) ) return true;

        return false;
      }
    }
  ]
});
