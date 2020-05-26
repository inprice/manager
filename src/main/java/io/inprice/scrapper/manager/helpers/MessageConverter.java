package io.inprice.scrapper.manager.helpers;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This is a helper class to convert Object to byte array and vice versa
 *
 * @author mdpinar
 */
public class MessageConverter {

  private static final Logger log = LoggerFactory.getLogger(MessageConverter.class);

  /**
   * From byte array to Object
   *
   */
  @SuppressWarnings("unchecked")
  public static <T> T toObject(byte[] byteArray) {
    try {
      ByteArrayInputStream in = new ByteArrayInputStream(byteArray);
      ObjectInputStream ois = new ObjectInputStream(in);

      return (T) ois.readObject();
    } catch (Exception e) {
      log.error("Error", e);
    }

    return null;
  }

  /**
   * From Object to byte array
   *
   */
  public static byte[] fromObject(Serializable object) {
    try {
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      ObjectOutputStream oos = new ObjectOutputStream(bos);
      oos.writeObject(object);
      oos.flush();
      oos.close();

      return bos.toByteArray();
    } catch (IOException e) {
      log.error("Error", e);
    }

    return null;
  }

}
