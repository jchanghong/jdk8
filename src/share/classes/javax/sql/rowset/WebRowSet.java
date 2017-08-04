

package javax.sql.rowset;

import java.sql.*;
import javax.sql.*;
import javax.naming.*;
import java.io.*;
import java.math.*;
import org.xml.sax.*;



public interface WebRowSet extends CachedRowSet {


    public void readXml(java.io.Reader reader) throws SQLException;


    public void readXml(java.io.InputStream iStream) throws SQLException, IOException;


    public void writeXml(ResultSet rs, java.io.Writer writer) throws SQLException;


    public void writeXml(ResultSet rs, java.io.OutputStream oStream) throws SQLException, IOException;


    public void writeXml(java.io.Writer writer) throws SQLException;


    public void writeXml(java.io.OutputStream oStream) throws SQLException, IOException;


    public static String PUBLIC_XML_SCHEMA =
        "--//Oracle Corporation//XSD Schema//EN";


    public static String SCHEMA_SYSTEM_ID = "http://java.sun.com/xml/ns/jdbc/webrowset.xsd";
}
