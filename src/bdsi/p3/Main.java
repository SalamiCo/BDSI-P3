package bdsi.p3;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

import org.exist.xmldb.DatabaseImpl;
import org.xmldb.api.DatabaseManager;
import org.xmldb.api.base.Collection;
import org.xmldb.api.base.Database;
import org.xmldb.api.base.Resource;
import org.xmldb.api.base.ResourceIterator;
import org.xmldb.api.base.ResourceSet;
import org.xmldb.api.modules.CollectionManagementService;
import org.xmldb.api.modules.XMLResource;
import org.xmldb.api.modules.XPathQueryService;

public class Main {
    
    private static final String URL_COLLECTION_BASE = "xmldb:exist://localhost:8080/exist/xmlrpc/db";
    
    private static final Charset UTF_8 = Charset.forName("UTF-8");
    
    private static void printUsage () {
        System.out.println("Modo de uso:");
        System.out.println("  java bdsi.p3.Main <coleccion> <agencias> <anuncios> <consultas> <salida>");
        System.out.println();
        System.out.println("  <coleccion>  Nombre de la colección.");
        System.out.println();
        System.out.println("   <agencias>  Ruta del fichero XML que define las agencias.");
        System.out.println();
        System.out.println("   <anuncios>  Ruta del fichero XML quedefine los anuncios (inmuebles).");
        System.out.println();
        System.out.println("  <consultas>  Ruta del fichero que contiene las consultas.");
        System.out.println("               Puede usarse - para leer la entrada de stdin.");
        System.out.println();
        System.out.println("     <salida>  Ruta del fichero de salida.");
        System.out.println("               Puede usarse - para mostrar la salida en stdout.");
    }
    
    private static BufferedReader openInput (String path) throws IOException {
        if (path.equals("-")) {
            return new BufferedReader(new InputStreamReader(System.in));
        } else {
            return Files.newBufferedReader(Paths.get(path), UTF_8);
        }
    }
    
    private static PrintWriter openOutput (String path) throws IOException {
        if (path.equals("-")) {
            return new PrintWriter(new OutputStreamWriter(System.out));
        } else {
            return new PrintWriter(Files.newBufferedWriter(Paths.get(path), UTF_8, StandardOpenOption.CREATE));
        }
    }
    
    public static void main (String[] args) {
        try {
            if (args.length != 5) {
                printUsage();
                
            } else {
                String argCollection = args[0];
                argCollection = argCollection.replaceFirst("^/(db/)?", "");
                
                String argAgencies = args[1];
                String argAdverts = args[2];
                String argQueries = args[3];
                String argOutput = args[4];
                
                Database db = new DatabaseImpl();
                db.setProperty("create-database", "true");
                DatabaseManager.registerDatabase(db);
                
                String user = "admin";
                String pass = "pechotes";
                
                String colUrl = URL_COLLECTION_BASE + "/" + argCollection;
                Collection collection = DatabaseManager.getCollection(colUrl, user, pass);
                
                // Creamos la colección si no existe
                if (collection == null) {
                    Collection root = DatabaseManager.getCollection(URL_COLLECTION_BASE, user, pass);
                    CollectionManagementService mgtService =
                        (CollectionManagementService) root.getService("CollectionManagementService", "1.0");
                    collection = mgtService.createCollection(argCollection);
                }
                
                // Cargamos y almacenamos las agencias
                Path agenciesPath = Paths.get(argAgencies);
                if (!Files.exists(agenciesPath) || !Files.isRegularFile(agenciesPath)) {
                    throw new FileNotFoundException(agenciesPath.toString());
                }
                XMLResource agenciesDoc = (XMLResource) collection.createResource(null, "XMLResource");
                agenciesDoc.setContent(agenciesPath.toFile());
                collection.storeResource(agenciesDoc);
                
                // Cargamos y almacenamos los anuncios
                Path advertsPath = Paths.get(argAdverts);
                if (!Files.exists(advertsPath) || !Files.isRegularFile(advertsPath)) {
                    throw new FileNotFoundException(advertsPath.toString());
                }
                XMLResource advertDoc = (XMLResource) collection.createResource(null, "XMLResource");
                advertDoc.setContent(advertsPath.toFile());
                collection.storeResource(advertDoc);
                
                XPathQueryService service = (XPathQueryService) collection.getService("XPathQueryService", "1.0");
                service.setProperty("pretty", "true");
                service.setProperty("encoding", "UTF-8");
                
                List<String> titles = new ArrayList<>();
                List<String> queries = new ArrayList<>();
                
                // Entrada
                try (BufferedReader input = openInput(argQueries)) {
                    // Leemos las consultas línea a línea
                    String line;
                    String title = null;
                    StringBuilder query = new StringBuilder();
                    while (null != (line = input.readLine())) {
                        line = line.trim();
                        
                        if (!line.equals("")) {
                            if (line.startsWith("//")) {
                                if (query.length() != 0) {
                                    titles.add(title);
                                    queries.add(query.toString().trim());
                                }
                                
                                query.setLength(0);
                                title = line.substring(2);
                                
                            } else {
                                query.append(line).append('\n');
                            }
                        }
                    }

                    if (query.length() != 0) {
                        titles.add(title);
                        queries.add(query.toString().trim());
                    }
                    
                }
                
                // Salida
                assert titles.size() == queries.size();
                try (PrintWriter output = openOutput(argOutput)) {
                    for (int i = 0; i < titles.size(); i++) {
                        ResourceSet result = service.query(queries.get(i));
                        
                        ResourceIterator it = result.getIterator();
                        while (it.hasMoreResources()) {
                            Resource res = it.nextResource();
                            
                            output.println(titles.get(i));
                            output.println("--------");
                            output.println("Input: -----");
                            output.println(queries.get(i));
                            output.println("Output:");
                            output.println(res.getContent());
                            output.println();
                            output.flush();
                        }
                    }
                }
            }
        } catch (Exception exc) {
            exc.printStackTrace();
        }
        
    }
}
