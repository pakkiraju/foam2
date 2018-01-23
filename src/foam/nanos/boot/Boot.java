/**
 * @license
 * Copyright 2017 The FOAM Authors. All Rights Reserved.
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package foam.nanos.boot;

import foam.core.*;
import foam.dao.AbstractSink;
import foam.dao.DAO;
import foam.dao.JDAO;
import foam.dao.ProxyDAO;
import foam.nanos.auth.User;
import foam.nanos.script.Script;
import foam.nanos.session.Session;
import org.apache.commons.io.IOUtils;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.util.Properties;

import static foam.mlang.MLang.EQ;

public class Boot {
  protected DAO serviceDAO_;
  protected X   root_ = new ProxyX();

  public Boot() {
    this("");
  }

  public Boot(String datadir) {
    root_.put(foam.nanos.fs.Storage.class,
              new foam.nanos.fs.Storage(datadir));

    // Used for all the services that will be required when Booting
    serviceDAO_ = new JDAO(((foam.core.ProxyX)root_).getX(), NSpec.getOwnClassInfo(), "services");

    installSystemUser();
    installKeyStore();

    serviceDAO_.select(new AbstractSink() {
      public void put(FObject obj, Detachable sub) {
        NSpec sp = (NSpec) obj;
        System.out.println("Registering: " + sp.getName());
        root_.putFactory(sp.getName(), new SingletonFactory(new NSpecFactory((ProxyX) root_, sp)));
      }
    });

    /**
     * Revert root_ to non ProxyX to avoid letting children add new bindings.
     */
    root_ = ((ProxyX) root_).getX();

    // Export the ServiceDAO
    ((ProxyDAO) root_.get("nSpecDAO")).setDelegate(
        new foam.dao.PMDAO(new foam.dao.AuthenticatedDAO("service", false, serviceDAO_)));

    serviceDAO_.where(EQ(NSpec.LAZY, false)).select(new AbstractSink() {
      public void put(FObject obj, Detachable sub) {
        NSpec sp = (NSpec) obj;

        System.out.println("Starting: " + sp.getName());
        root_.get(sp.getName());
      }
    });

    String startScript = System.getProperty("foam.main", "main");
    if ( startScript != null ) {
      DAO    scriptDAO = (DAO) root_.get("scriptDAO");
      Script script    = (Script) scriptDAO.find(startScript);
      if ( script != null ) {
        script.runScript(root_);
      }
    }
  }

  protected void installSystemUser() {
    User user = new User();
    user.setId(1);
    user.setFirstName("system");
    user.setGroup("system");

    Session session = new Session();
    session.setUserId(user.getId());
    session.setContext(root_);

    root_.put("user", user);
    root_.put(Session.class, session);
  }

  /**
   * Loads a keystore file and stores it in the context using a factory
   */
  protected void installKeyStore() {
    root_.putFactory(KeyStore.class, new XFactory() {
      @Override
      public Object create(X x) {
        FileInputStream configIn = null;
        FileInputStream keyStoreIn = null;

        try {
          // load config.properties
          configIn = new FileInputStream("config.properties");

          // load properties file and get password
          Properties properties = new Properties();
          properties.load(configIn);
          char[] password = properties.
              getProperty("keystore.password").toCharArray();

          // load keystore using password
          keyStoreIn = new FileInputStream("keystore.jks");
          KeyStore keystore = KeyStore.getInstance("BKS", "BC");
          keystore.load(keyStoreIn, password);
          return keystore;
        } catch (Throwable t) {
          throw new RuntimeException(t);
        } finally {
          IOUtils.closeQuietly(configIn);
          IOUtils.closeQuietly(keyStoreIn);
        }
      }
    });
  }

  public X getX() { return root_; }

  public static void main (String[] args)
      throws java.lang.Exception
  {
    System.out.println("Starting Nanos Server");

    boolean datadirFlag = false;

    String datadir = "";
    for ( int i = 0 ; i < args.length ; i++ ) {
      String arg = args[i];

      if ( datadirFlag ) {
        datadir = arg;
        datadirFlag = false;
      } else if ( arg.equals("--datadir") ) {
        datadirFlag = true;
      } else {
        System.err.println("Unknown argument " + arg);
        System.exit(1);
      }
    }

    System.out.println("Datadir is " + datadir);

    new Boot(datadir);
  }
}
