package bdsi.p3;

import java.io.File;

import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.XMLDBException;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;

public class Main {

	/**
	 * @param args
	 * @throws XMLDBException
	 */
	public static void main(String[] args) throws XMLDBException {
		// Se inicializa el driver
		// String driver = "org.exist.xmldb.DatabaseImpl";

		// Cargar el driver
		// Class cl = Class.forName(driver);//Cargar el driver

		// Instancia de la BD
		Database database = new org.exist.xmldb.DatabaseImpl();
		database.setProperty("create-database", "true");

		// Registrar la BD
		DatabaseManager.registerDatabase(database);

		// ---------------------------------------------------------------------
		// Se intenta acceder a la coleccion especificada como primer argumento
		// ---------------------------------------------------------------------
		String collection = /*args[0]*/"pene";
		String file = /*args[1]*/"agencias.xml";

		// Eliminar "/db" si es especificado en la entrada
		if (collection.startsWith("/db")) {
			collection = collection.substring(3);
		}
		String urlBD1 = "xmldb:exist://localhost:8080/exist/xmlrpc/db" + collection;
		Collection col = DatabaseManager.getCollection(urlBD1, "user", "password");
		// Si la coleccion especificada como primer argumento no existe,
		// entonces se crea
		if (col == null) {
			String urlBD2 = "xmldb:exist://localhost:8080/exist/xmlrpc/db";
			Collection root = DatabaseManager.getCollection(urlBD2, "admin", "password");
			CollectionManagementService mgtService = (CollectionManagementService) root.getService(
				"CollectionManagementService", "1.0");
			col = mgtService.createCollection(collection);
		}
		// ---------------------------------------------------
		// Se crea un recurso XML para almacenar el documento
		// --------------------------------------------------
		XMLResource document = (XMLResource) col.createResource(null, "XMLResource");
		File f = new File(file);
		if (!f.canRead()) {
			System.err.println("No es posible leer el archivo " + file);
			System.out.println(f.toString());
			document.setContent(f);
			System.out.println("Almacenando el archivo " + document.getId() + "...");
			col.storeResource(document);
			System.out.println("Almacenado correctamente");
		}
	}
}
