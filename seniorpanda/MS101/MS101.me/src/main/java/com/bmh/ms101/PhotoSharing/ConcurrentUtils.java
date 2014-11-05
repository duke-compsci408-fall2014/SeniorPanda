package com.bmh.ms101.PhotoSharing;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created on 11/5/14
 */
public class ConcurrentUtils {

    public static String SerializeHashMap(Map<String, String> map){
        try
        {
            FileOutputStream fos = new FileOutputStream("hashmap.ser");
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(map);
            oos.close();
            fos.close();
            System.out.printf("Serialized HashMap data is saved in hashmap.ser");
            return new String("hashmap.ser");

        }
        catch(IOException ioe)
        {
            ioe.printStackTrace();
        }
        return null;
    }

    public static Map<String, String> DeserializeHashMap(Serializable file){
        HashMap<String, String> map = null;
        try{
            assert(file.equals("hashmap.ser"));
            // check to see if the name is hashmap.ser

            FileInputStream fis = new FileInputStream("hashmap.ser");
            ObjectInputStream ois = new ObjectInputStream(fis);
            map = (HashMap) ois.readObject();
            ois.close();
            fis.close();
            System.out.println("Deserialized HashMap..");
            return map;
        }catch(IOException ioe)
        {
            ioe.printStackTrace();
            return null;
        }catch(ClassNotFoundException c)
        {
            System.out.println("Class not found");
            c.printStackTrace();
            return null;
        }

    }


}
