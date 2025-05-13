package com.YCorp.chessApp.server.db;

public class databaseTests {
    public static void createTableDatabaseTest(){
        System.out.println("Service Exist?: " + RegexDatabase.doesServiceExist("mysql84"));
        try{
            System.out.println("Service Running?: " + RegexDatabase.isServiceRunning("mysql84"));
        }
        catch (Exception e){
            System.out.println("Exception occurred exectuing isServiceRunning()");
        }

        System.out.println("Database Exist?: " + RegexDatabase.doesDatabaseExist());
        System.out.println("Creating database");
        RegexDatabase.createDatabase();
        System.out.println("Database Exist now?: " + RegexDatabase.doesDatabaseExist());

        System.out.println("Table Exist?: " + RegexDatabase.doesTableExist());
        System.out.println("Creating table");
        RegexDatabase.createTable();
        System.out.println("Table Exist now?: " + RegexDatabase.doesTableExist());
    }
}
