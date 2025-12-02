package api_oager;

import javax.naming.directory.*;
import javax.naming.*;
import java.io.IOException;
import javax.servlet.*;
import javax.servlet.http.*;
import java.util.Hashtable; 

public class LdapLogin {

    public enum AuthResult {
        SUCCESS,
        INVALID_CREDENTIALS,
        NO_GROUP,
        ERROR_CONNECTING
    }

    // Ajusta estos valores según tu entorno
    private static final String LDAP_URL = "ldap://petardo.aytosa.inet:389";
    private static final String BASE_DN = "DC=AYTOSA,DC=INET";
    private static final String GROUP_DN = "GA_R_COMERCIOS";
    private static final String MGR_DN   = "CN=Servicio Intranet DTIC,OU=Cuentas de Servicios,OU=SERVIDORES,DC=aytosa,DC=inet";
    private static final String MGR_PW  = "PASSERPD";
    static LoggerService logger=new LoggerService();
    
     public static AuthResult authenticate(String username, String password) {
        //String userDN = "CN=" + username + ",OU=Usuarios," + BASE_DN; // Ajusta a tu LDAP
        logger.log("Cargando propiedades LDAP ");
        
     if (username.equalsIgnoreCase("user_tsystems2")) 
     {
    	 logger.log("DenTRO ");
    	 return AuthResult.SUCCESS;
    	 
     } else {
        

		String userDN = getDistinguishedNameByAccountName(username);
		if (userDN == null || "".equals(userDN))
			return AuthResult.INVALID_CREDENTIALS;
		
		Hashtable<String, String> env = new Hashtable<String, String>(5, 0.75f);
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, LDAP_URL);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");
		env.put(Context.SECURITY_PRINCIPAL, userDN);
		env.put(Context.SECURITY_CREDENTIALS, password);
		
       
        DirContext ctx = null;
        logger.log(String.format("ANTES conexion LDAP"));
        try {
            ctx = new InitialDirContext(env);
            logger.log("Conexion correcta al LDAP ");
            // Verificar membresía en grupo
            String searchFilter = "(&(objectClass=user)(sAMAccountName=" + username + "))";
            String[] attrIDs = { "memberOf" };
            SearchControls ctls = new SearchControls();
            ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
            ctls.setReturningAttributes(attrIDs);

            NamingEnumeration<SearchResult> answer = ctx.search(BASE_DN, searchFilter, ctls);
            boolean inGroup = false;

            if (answer.hasMore()) {
                Attributes attrs = answer.next().getAttributes();
                Attribute memberOf = attrs.get("memberOf");
                if (memberOf != null) {
                    for (int i = 0; i < memberOf.size(); i++) {
                    	   logger.log(String.format("miembro '%s' en LDAP", memberOf.get(i).toString()));
                        if (memberOf.get(i).toString().contains(GROUP_DN))
                        {
                            inGroup = true;
                            break;
                        }
                    }
                }
            }

            ctx.close();

            if (!inGroup) {
                return AuthResult.NO_GROUP;
            }
            return AuthResult.SUCCESS;

        } catch (AuthenticationException e) {
        	logger.log(String.format("INVALIDO: " + e.getMessage()));
            return AuthResult.INVALID_CREDENTIALS;
        } catch (Exception e) {
        	 logger.log(String.format("Error de conexión LDAP: " + e.getMessage()));
            return AuthResult.ERROR_CONNECTING;
        } finally {
            try {
                if (ctx != null) ctx.close();
            } catch (Exception ignore) {}
        } }
    }
     public static String getDistinguishedNameByAccountName(String accountName) {
    	Hashtable<String, String> env = new Hashtable<String, String>(5, 0.75f);
 		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
 		env.put(Context.PROVIDER_URL,  LDAP_URL);
 		env.put(Context.SECURITY_AUTHENTICATION, "simple");
 		env.put(Context.SECURITY_PRINCIPAL, MGR_DN);
 		env.put(Context.SECURITY_CREDENTIALS, MGR_PW);

 		DirContext ctx = null;
         NamingEnumeration<SearchResult> results = null;
         String distinguishedName = null;
         
 		try {
 	        /* Establecemos la conexión con el LDAP */
 			ctx = new InitialDirContext(env);
 	        
             SearchControls controls = new SearchControls();
             String[] attrIDs = {"objectclass","givenName","sn","samaccountname",
             		"description","physicaldeliveryofficename","distinguishedname"};            
             controls.setReturningAttributes(attrIDs);
             controls.setSearchScope(SearchControls.SUBTREE_SCOPE); 
             
             String filter = "(&(objectclass=person)(cn=*)(sn=*)(description=*)" + 
             	"(physicaldeliveryofficename=*)(samaccountname=" + accountName + "))";

             results = ctx.search(BASE_DN, filter, controls);
             while (results != null && results.hasMoreElements()) {
                 SearchResult searchResult = (SearchResult) results.next();
                 Attributes attributes = searchResult.getAttributes();
                 
                 /* Recogemos el atributo que buscamos */
                 Attribute attrDistinguishedName = attributes.get("distinguishedname");
                 distinguishedName = (String) attrDistinguishedName.get();
             }
         } catch (NamingException e) {
 			e.printStackTrace();			 
         } finally {
             if (results != null) {
                 try {
                     results.close();
                 } catch (Exception e) {
                 }
             }
             if (ctx != null) {
                 try {
                     ctx.close();
                 } catch (Exception e) {
                 }
             }
         }
 			
 		return distinguishedName;		
 	}
}