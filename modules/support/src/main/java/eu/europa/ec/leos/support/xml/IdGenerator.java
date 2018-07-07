/**
 * Copyright 2016 European Commission
 *
 * Licensed under the EUPL, Version 1.1 or – as soon they will be approved by the European Commission - subsequent versions of the EUPL (the "Licence");
 * You may not use this work except in compliance with the Licence.
 * You may obtain a copy of the Licence at:
 *
 *     https://joinup.ec.europa.eu/software/page/eupl
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and limitations under the Licence.
 */
package eu.europa.ec.leos.support.xml;

import org.apache.commons.lang3.RandomStringUtils;

public class IdGenerator {
      
     public static final int DEFAULT_POSTFIX_LEN=7;
     public static final String DEFAULT_PREFIX="akn";
     
      public static String generateId(int length){
          return RandomStringUtils.randomAlphanumeric(length);
      }
      
      /** 
       * Generates an id in format (generated id=prefix + (7 Chars long String))
       * @param prefix String to prefix with Random id
       * @return ( generated id=prefix + (7 Chars long String)) 
       */
      public static String generateId(String prefix){
          int postfixLength=DEFAULT_POSTFIX_LEN;
          return generateId(prefix, postfixLength);
          
      }
      /** 
       * Generates an id in format (generated id=prefix + ( String of length postfixLength)
       * @param prefix String to prefix with Random id
       * @param postfixLength length of String to be appended
       * @return ( generatedId= prefix + (Random String of length postfixLength)) 
       */
      public static String generateId(String prefix, int postfixLength){
          StringBuffer sb =new StringBuffer();
          
          if(prefix!=null){
              sb.append(prefix).append("_");
              postfixLength--;
          }
          
          return sb.append(RandomStringUtils.randomAlphanumeric(postfixLength)).toString();
      }
      
}
