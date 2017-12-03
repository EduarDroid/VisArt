/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.vision.artificial.model;

import java.sql.*;

/**
 *
 * @author EduardoDev
 */
public class EntityModel {
    //private final String tabla = "tareas";
    public static void guardarLimon(Connection conexion, Limon limon) throws SQLException{
      try{
        CallableStatement query=  conexion.prepareCall("call procesarLimon(?,?,?,?);");
        query.setInt(1, limon.getEjecucion());        
        query.setInt(2, limon.getIdentificador());
        query.setDouble(3, limon.getDiametroA());
        query.setDouble(4, limon.getDiametroB());    
        query.execute();
        //limon.setId(query.getInt(1));
      }catch(SQLException ex){
         throw new SQLException(ex);
      }
    }
    
    public static void guardarEjecucion(Connection conexion, Ejecucion ejecucion) throws SQLException{
      try{  
        CallableStatement query=  conexion.prepareCall("call guardarEjecucion(?);");
        query.registerOutParameter(1, java.sql.Types.INTEGER);
        query.execute();
        ejecucion.setId(query.getInt(1));
      }catch(SQLException ex){
         throw new SQLException(ex);
      }
    }
}
