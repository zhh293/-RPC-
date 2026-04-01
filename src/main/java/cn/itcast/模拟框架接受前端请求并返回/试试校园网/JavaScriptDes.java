package cn.itcast.模拟框架接受前端请求并返回.试试校园网;

public class JavaScriptDes {
    public static final String javascriptCode= """
            /**\s
            * DES加密解密\s
            * @Copyright Copyright (c) 2006\s
            * @author Guapo\s
            * @see DESCore\s
            */ \s
             \s
            /*\s
            * encrypt the string to string made up of hex\s
            * return the encrypted string\s
            */ \s
            function strEnc(data,firstKey,secondKey,thirdKey){ \s
             \s
             var leng = data.length; \s
             var encData = ""; \s
             var firstKeyBt,secondKeyBt,thirdKeyBt,firstLength,secondLength,thirdLength; \s
             if(firstKey != null && firstKey != ""){     \s
               firstKeyBt = getKeyBytes(firstKey); \s
               firstLength = firstKeyBt.length; \s
             } \s
             if(secondKey != null && secondKey != ""){ \s
               secondKeyBt = getKeyBytes(secondKey); \s
               secondLength = secondKeyBt.length; \s
             } \s
             if(thirdKey != null && thirdKey != ""){ \s
               thirdKeyBt = getKeyBytes(thirdKey); \s
               thirdLength = thirdKeyBt.length; \s
             }   \s
              \s
             if(leng > 0){ \s
               if(leng < 4){ \s
                 var bt = strToBt(data);       \s
                 var encByte ; \s
                 if(firstKey != null && firstKey !="" && secondKey != null && secondKey != "" && thirdKey != null && thirdKey != ""){ \s
                   var tempBt; \s
                   var x,y,z; \s
                   tempBt = bt;         \s
                   for(x = 0;x < firstLength ;x ++){ \s
                     tempBt = enc(tempBt,firstKeyBt[x]); \s
                   } \s
                   for(y = 0;y < secondLength ;y ++){ \s
                     tempBt = enc(tempBt,secondKeyBt[y]); \s
                   } \s
                   for(z = 0;z < thirdLength ;z ++){ \s
                     tempBt = enc(tempBt,thirdKeyBt[z]); \s
                   }         \s
                   encByte = tempBt;         \s
                 }else{ \s
                   if(firstKey != null && firstKey !="" && secondKey != null && secondKey != ""){ \s
                     var tempBt; \s
                     var x,y; \s
                     tempBt = bt; \s
                     for(x = 0;x < firstLength ;x ++){ \s
                       tempBt = enc(tempBt,firstKeyBt[x]); \s
                     } \s
                     for(y = 0;y < secondLength ;y ++){ \s
                       tempBt = enc(tempBt,secondKeyBt[y]); \s
                     } \s
                     encByte = tempBt; \s
                   }else{ \s
                     if(firstKey != null && firstKey !=""){             \s
                       var tempBt; \s
                       var x = 0; \s
                       tempBt = bt;             \s
                       for(x = 0;x < firstLength ;x ++){ \s
                         tempBt = enc(tempBt,firstKeyBt[x]); \s
                       } \s
                       encByte = tempBt; \s
                     } \s
                   }         \s
                 } \s
                 encData = bt64ToHex(encByte); \s
               }else{ \s
                 var iterator = parseInt(leng/4); \s
                 var remainder = leng%4; \s
                 var i=0;       \s
                 for(i = 0;i < iterator;i++){ \s
                   var tempData = data.substring(i*4+0,i*4+4); \s
                   var tempByte = strToBt(tempData); \s
                   var encByte ; \s
                   if(firstKey != null && firstKey !="" && secondKey != null && secondKey != "" && thirdKey != null && thirdKey != ""){ \s
                     var tempBt; \s
                     var x,y,z; \s
                     tempBt = tempByte; \s
                     for(x = 0;x < firstLength ;x ++){ \s
                       tempBt = enc(tempBt,firstKeyBt[x]); \s
                     } \s
                     for(y = 0;y < secondLength ;y ++){ \s
                       tempBt = enc(tempBt,secondKeyBt[y]); \s
                     } \s
                     for(z = 0;z < thirdLength ;z ++){ \s
                       tempBt = enc(tempBt,thirdKeyBt[z]); \s
                     } \s
                     encByte = tempBt; \s
                   }else{ \s
                     if(firstKey != null && firstKey !="" && secondKey != null && secondKey != ""){ \s
                       var tempBt; \s
                       var x,y; \s
                       tempBt = tempByte; \s
                       for(x = 0;x < firstLength ;x ++){ \s
                         tempBt = enc(tempBt,firstKeyBt[x]); \s
                       } \s
                       for(y = 0;y < secondLength ;y ++){ \s
                         tempBt = enc(tempBt,secondKeyBt[y]); \s
                       } \s
                       encByte = tempBt; \s
                     }else{ \s
                       if(firstKey != null && firstKey !=""){                       \s
                         var tempBt; \s
                         var x; \s
                         tempBt = tempByte; \s
                         for(x = 0;x < firstLength ;x ++){                 \s
                           tempBt = enc(tempBt,firstKeyBt[x]); \s
                         } \s
                         encByte = tempBt;               \s
                       } \s
                     } \s
                   } \s
                   encData += bt64ToHex(encByte); \s
                 }       \s
                 if(remainder > 0){ \s
                   var remainderData = data.substring(iterator*4+0,leng); \s
                   var tempByte = strToBt(remainderData); \s
                   var encByte ; \s
                   if(firstKey != null && firstKey !="" && secondKey != null && secondKey != "" && thirdKey != null && thirdKey != ""){ \s
                     var tempBt; \s
                     var x,y,z; \s
                     tempBt = tempByte; \s
                     for(x = 0;x < firstLength ;x ++){ \s
                       tempBt = enc(tempBt,firstKeyBt[x]); \s
                     } \s
                     for(y = 0;y < secondLength ;y ++){ \s
                       tempBt = enc(tempBt,secondKeyBt[y]); \s
                     } \s
                     for(z = 0;z < thirdLength ;z ++){ \s
                       tempBt = enc(tempBt,thirdKeyBt[z]); \s
                     } \s
                     encByte = tempBt; \s
                   }else{ \s
                     if(firstKey != null && firstKey !="" && secondKey != null && secondKey != ""){ \s
                       var tempBt; \s
                       var x,y; \s
                       tempBt = tempByte; \s
                       for(x = 0;x < firstLength ;x ++){ \s
                         tempBt = enc(tempBt,firstKeyBt[x]); \s
                       } \s
                       for(y = 0;y < secondLength ;y ++){ \s
                         tempBt = enc(tempBt,secondKeyBt[y]); \s
                       } \s
                       encByte = tempBt; \s
                     }else{ \s
                       if(firstKey != null && firstKey !=""){             \s
                         var tempBt; \s
                         var x; \s
                         tempBt = tempByte; \s
                         for(x = 0;x < firstLength ;x ++){ \s
                           tempBt = enc(tempBt,firstKeyBt[x]); \s
                         } \s
                         encByte = tempBt; \s
                       } \s
                     } \s
                   } \s
                   encData += bt64ToHex(encByte); \s
                 }                 \s
               } \s
             } \s
             return encData; \s
            } \s
             \s
            /*\s
            * decrypt the encrypted string to the original string \s
            *\s
            * return  the original string  \s
            */ \s
            function strDec(data,firstKey,secondKey,thirdKey){ \s
             var leng = data.length; \s
             var decStr = ""; \s
             var firstKeyBt,secondKeyBt,thirdKeyBt,firstLength,secondLength,thirdLength; \s
             if(firstKey != null && firstKey != ""){     \s
               firstKeyBt = getKeyBytes(firstKey); \s
               firstLength = firstKeyBt.length; \s
             } \s
             if(secondKey != null && secondKey != ""){ \s
               secondKeyBt = getKeyBytes(secondKey); \s
               secondLength = secondKeyBt.length; \s
             } \s
             if(thirdKey != null && thirdKey != ""){ \s
               thirdKeyBt = getKeyBytes(thirdKey); \s
               thirdLength = thirdKeyBt.length; \s
             } \s
              \s
             var iterator = parseInt(leng/16); \s
             var i=0;   \s
             for(i = 0;i < iterator;i++){ \s
               var tempData = data.substring(i*16+0,i*16+16);     \s
               var strByte = hexToBt64(tempData);     \s
               var intByte = new Array(64); \s
               var j = 0; \s
               for(j = 0;j < 64; j++){ \s
                 intByte[j] = parseInt(strByte.substring(j,j+1)); \s
               }     \s
               var decByte; \s
               if(firstKey != null && firstKey !="" && secondKey != null && secondKey != "" && thirdKey != null && thirdKey != ""){ \s
                 var tempBt; \s
                 var x,y,z; \s
                 tempBt = intByte; \s
                 for(x = thirdLength - 1;x >= 0;x --){ \s
                   tempBt = dec(tempBt,thirdKeyBt[x]); \s
                 } \s
                 for(y = secondLength - 1;y >= 0;y --){ \s
                   tempBt = dec(tempBt,secondKeyBt[y]); \s
                 } \s
                 for(z = firstLength - 1;z >= 0 ;z --){ \s
                   tempBt = dec(tempBt,firstKeyBt[z]); \s
                 } \s
                 decByte = tempBt; \s
               }else{ \s
                 if(firstKey != null && firstKey !="" && secondKey != null && secondKey != ""){ \s
                   var tempBt; \s
                   var x,y,z; \s
                   tempBt = intByte; \s
                   for(x = secondLength - 1;x >= 0 ;x --){ \s
                     tempBt = dec(tempBt,secondKeyBt[x]); \s
                   } \s
                   for(y = firstLength - 1;y >= 0 ;y --){ \s
                     tempBt = dec(tempBt,firstKeyBt[y]); \s
                   } \s
                   decByte = tempBt; \s
                 }else{ \s
                   if(firstKey != null && firstKey !=""){ \s
                     var tempBt; \s
                     var x,y,z; \s
                     tempBt = intByte; \s
                     for(x = firstLength - 1;x >= 0 ;x --){ \s
                       tempBt = dec(tempBt,firstKeyBt[x]); \s
                     } \s
                     decByte = tempBt; \s
                   } \s
                 } \s
               } \s
               decStr += byteToString(decByte); \s
             }       \s
             return decStr; \s
            } \s
            /*\s
            * chang the string into the bit array\s
            * \s
            * return bit array(it's length % 64 = 0)\s
            */ \s
            function getKeyBytes(key){ \s
             var keyBytes = new Array(); \s
             var leng = key.length; \s
             var iterator = parseInt(leng/4); \s
             var remainder = leng%4; \s
             var i = 0; \s
             for(i = 0;i < iterator; i ++){ \s
               keyBytes[i] = strToBt(key.substring(i*4+0,i*4+4)); \s
             } \s
             if(remainder > 0){ \s
               keyBytes[i] = strToBt(key.substring(i*4+0,leng)); \s
             }     \s
             return keyBytes; \s
            } \s
             \s
            /*\s
            * chang the string(it's length <= 4) into the bit array\s
            * \s
            * return bit array(it's length = 64)\s
            */ \s
            function strToBt(str){   \s
             var leng = str.length; \s
             var bt = new Array(64); \s
             if(leng < 4){ \s
               var i=0,j=0,p=0,q=0; \s
               for(i = 0;i<leng;i++){ \s
                 var k = str.charCodeAt(i); \s
                 for(j=0;j<16;j++){       \s
                   var pow=1,m=0; \s
                   for(m=15;m>j;m--){ \s
                     pow *= 2; \s
                   }         \s
                   bt[16*i+j]=parseInt(k/pow)%2; \s
                 } \s
               } \s
               for(p = leng;p<4;p++){ \s
                 var k = 0; \s
                 for(q=0;q<16;q++){       \s
                   var pow=1,m=0; \s
                   for(m=15;m>q;m--){ \s
                     pow *= 2; \s
                   }         \s
                   bt[16*p+q]=parseInt(k/pow)%2; \s
                 } \s
               }   \s
             }else{ \s
               for(i = 0;i<4;i++){ \s
                 var k = str.charCodeAt(i); \s
                 for(j=0;j<16;j++){       \s
                   var pow=1; \s
                   for(m=15;m>j;m--){ \s
                     pow *= 2; \s
                   }         \s
                   bt[16*i+j]=parseInt(k/pow)%2; \s
                 } \s
               }   \s
             } \s
             return bt; \s
            } \s
             \s
            /*\s
            * chang the bit(it's length = 4) into the hex\s
            * \s
            * return hex\s
            */ \s
            function bt4ToHex(binary) { \s
             var hex; \s
             switch (binary) { \s
               case "0000" : hex = "0"; break; \s
               case "0001" : hex = "1"; break; \s
               case "0010" : hex = "2"; break; \s
               case "0011" : hex = "3"; break; \s
               case "0100" : hex = "4"; break; \s
               case "0101" : hex = "5"; break; \s
               case "0110" : hex = "6"; break; \s
               case "0111" : hex = "7"; break; \s
               case "1000" : hex = "8"; break; \s
               case "1001" : hex = "9"; break; \s
               case "1010" : hex = "A"; break; \s
               case "1011" : hex = "B"; break; \s
               case "1100" : hex = "C"; break; \s
               case "1101" : hex = "D"; break; \s
               case "1110" : hex = "E"; break; \s
               case "1111" : hex = "F"; break; \s
             } \s
             return hex; \s
            } \s
             \s
            /*\s
            * chang the hex into the bit(it's length = 4)\s
            * \s
            * return the bit(it's length = 4)\s
            */ \s
            function hexToBt4(hex) { \s
             var binary; \s
             switch (hex) { \s
               case "0" : binary = "0000"; break; \s
               case "1" : binary = "0001"; break; \s
               case "2" : binary = "0010"; break; \s
               case "3" : binary = "0011"; break; \s
               case "4" : binary = "0100"; break; \s
               case "5" : binary = "0101"; break; \s
               case "6" : binary = "0110"; break; \s
               case "7" : binary = "0111"; break; \s
               case "8" : binary = "1000"; break; \s
               case "9" : binary = "1001"; break; \s
               case "A" : binary = "1010"; break; \s
               case "B" : binary = "1011"; break; \s
               case "C" : binary = "1100"; break; \s
               case "D" : binary = "1101"; break; \s
               case "E" : binary = "1110"; break; \s
               case "F" : binary = "1111"; break; \s
             } \s
             return binary; \s
            } \s
             \s
            /*\s
            * chang the bit(it's length = 64) into the string\s
            * \s
            * return string\s
            */ \s
            function byteToString(byteData){ \s
             var str=""; \s
             for(i = 0;i<4;i++){ \s
               var count=0; \s
               for(j=0;j<16;j++){         \s
                 var pow=1; \s
                 for(m=15;m>j;m--){ \s
                   pow*=2; \s
                 }               \s
                 count+=byteData[16*i+j]*pow; \s
               }         \s
               if(count != 0){ \s
                 str+=String.fromCharCode(count); \s
               } \s
             } \s
             return str; \s
            } \s
             \s
            function bt64ToHex(byteData){ \s
             var hex = ""; \s
             for(i = 0;i<16;i++){ \s
               var bt = ""; \s
               for(j=0;j<4;j++){     \s
                 bt += byteData[i*4+j]; \s
               }     \s
               hex+=bt4ToHex(bt); \s
             } \s
             return hex; \s
            } \s
             \s
            function hexToBt64(hex){ \s
             var binary = ""; \s
             for(i = 0;i<16;i++){ \s
               binary+=hexToBt4(hex.substring(i,i+1)); \s
             } \s
             return binary; \s
            } \s
             \s
            /*\s
            * the 64 bit des core arithmetic\s
            */ \s
             \s
            function enc(dataByte,keyByte){   \s
             var keys = generateKeys(keyByte);     \s
             var ipByte   = initPermute(dataByte);   \s
             var ipLeft   = new Array(32); \s
             var ipRight  = new Array(32); \s
             var tempLeft = new Array(32); \s
             var i = 0,j = 0,k = 0,m = 0, n = 0; \s
             for(k = 0;k < 32;k ++){ \s
               ipLeft[k] = ipByte[k]; \s
               ipRight[k] = ipByte[32+k]; \s
             }     \s
             for(i = 0;i < 16;i ++){ \s
               for(j = 0;j < 32;j ++){ \s
                 tempLeft[j] = ipLeft[j]; \s
                 ipLeft[j] = ipRight[j];       \s
               }   \s
               var key = new Array(48); \s
               for(m = 0;m < 48;m ++){ \s
                 key[m] = keys[i][m]; \s
               } \s
               var  tempRight = xor(pPermute(sBoxPermute(xor(expandPermute(ipRight),key))), tempLeft);       \s
               for(n = 0;n < 32;n ++){ \s
                 ipRight[n] = tempRight[n]; \s
               }   \s
                \s
             }   \s
              \s
              \s
             var finalData =new Array(64); \s
             for(i = 0;i < 32;i ++){ \s
               finalData[i] = ipRight[i]; \s
               finalData[32+i] = ipLeft[i]; \s
             } \s
             return finallyPermute(finalData);   \s
            } \s
             \s
            function dec(dataByte,keyByte){   \s
             var keys = generateKeys(keyByte);     \s
             var ipByte   = initPermute(dataByte);   \s
             var ipLeft   = new Array(32); \s
             var ipRight  = new Array(32); \s
             var tempLeft = new Array(32); \s
             var i = 0,j = 0,k = 0,m = 0, n = 0; \s
             for(k = 0;k < 32;k ++){ \s
               ipLeft[k] = ipByte[k]; \s
               ipRight[k] = ipByte[32+k]; \s
             }   \s
             for(i = 15;i >= 0;i --){ \s
               for(j = 0;j < 32;j ++){ \s
                 tempLeft[j] = ipLeft[j]; \s
                 ipLeft[j] = ipRight[j];       \s
               }   \s
               var key = new Array(48); \s
               for(m = 0;m < 48;m ++){ \s
                 key[m] = keys[i][m]; \s
               } \s
                \s
               var  tempRight = xor(pPermute(sBoxPermute(xor(expandPermute(ipRight),key))), tempLeft);       \s
               for(n = 0;n < 32;n ++){ \s
                 ipRight[n] = tempRight[n]; \s
               }   \s
             }   \s
              \s
              \s
             var finalData =new Array(64); \s
             for(i = 0;i < 32;i ++){ \s
               finalData[i] = ipRight[i]; \s
               finalData[32+i] = ipLeft[i]; \s
             } \s
             return finallyPermute(finalData);   \s
            } \s
             \s
            function initPermute(originalData){ \s
             var ipByte = new Array(64); \s
             for (i = 0, m = 1, n = 0; i < 4; i++, m += 2, n += 2) { \s
               for (j = 7, k = 0; j >= 0; j--, k++) { \s
                 ipByte[i * 8 + k] = originalData[j * 8 + m]; \s
                 ipByte[i * 8 + k + 32] = originalData[j * 8 + n]; \s
               } \s
             }     \s
             return ipByte; \s
            } \s
             \s
            function expandPermute(rightData){   \s
             var epByte = new Array(48); \s
             for (i = 0; i < 8; i++) { \s
               if (i == 0) { \s
                 epByte[i * 6 + 0] = rightData[31]; \s
               } else { \s
                 epByte[i * 6 + 0] = rightData[i * 4 - 1]; \s
               } \s
               epByte[i * 6 + 1] = rightData[i * 4 + 0]; \s
               epByte[i * 6 + 2] = rightData[i * 4 + 1]; \s
               epByte[i * 6 + 3] = rightData[i * 4 + 2]; \s
               epByte[i * 6 + 4] = rightData[i * 4 + 3]; \s
               if (i == 7) { \s
                 epByte[i * 6 + 5] = rightData[0]; \s
               } else { \s
                 epByte[i * 6 + 5] = rightData[i * 4 + 4]; \s
               } \s
             }       \s
             return epByte; \s
            } \s
             \s
            function xor(byteOne,byteTwo){   \s
             var xorByte = new Array(byteOne.length); \s
             for(i = 0;i < byteOne.length; i ++){       \s
               xorByte[i] = byteOne[i] ^ byteTwo[i]; \s
             }   \s
             return xorByte; \s
            } \s
             \s
            function sBoxPermute(expandByte){ \s
              \s
               var sBoxByte = new Array(32); \s
               var binary = ""; \s
               var s1 = [ \s
                   [14, 4, 13, 1, 2, 15, 11, 8, 3, 10, 6, 12, 5, 9, 0, 7], \s
                   [0, 15, 7, 4, 14, 2, 13, 1, 10, 6, 12, 11, 9, 5, 3, 8], \s
                   [4, 1, 14, 8, 13, 6, 2, 11, 15, 12, 9, 7, 3, 10, 5, 0], \s
                   [15, 12, 8, 2, 4, 9, 1, 7, 5, 11, 3, 14, 10, 0, 6, 13 ]]; \s
             \s
                   /* Table - s2 */ \s
               var s2 = [ \s
                   [15, 1, 8, 14, 6, 11, 3, 4, 9, 7, 2, 13, 12, 0, 5, 10], \s
                   [3, 13, 4, 7, 15, 2, 8, 14, 12, 0, 1, 10, 6, 9, 11, 5], \s
                   [0, 14, 7, 11, 10, 4, 13, 1, 5, 8, 12, 6, 9, 3, 2, 15], \s
                   [13, 8, 10, 1, 3, 15, 4, 2, 11, 6, 7, 12, 0, 5, 14, 9 ]]; \s
             \s
                   /* Table - s3 */ \s
               var s3= [ \s
                   [10, 0, 9, 14, 6, 3, 15, 5, 1, 13, 12, 7, 11, 4, 2, 8], \s
                   [13, 7, 0, 9, 3, 4, 6, 10, 2, 8, 5, 14, 12, 11, 15, 1], \s
                   [13, 6, 4, 9, 8, 15, 3, 0, 11, 1, 2, 12, 5, 10, 14, 7], \s
                   [1, 10, 13, 0, 6, 9, 8, 7, 4, 15, 14, 3, 11, 5, 2, 12 ]]; \s
                   /* Table - s4 */ \s
               var s4 = [ \s
                   [7, 13, 14, 3, 0, 6, 9, 10, 1, 2, 8, 5, 11, 12, 4, 15], \s
                   [13, 8, 11, 5, 6, 15, 0, 3, 4, 7, 2, 12, 1, 10, 14, 9], \s
                   [10, 6, 9, 0, 12, 11, 7, 13, 15, 1, 3, 14, 5, 2, 8, 4], \s
                   [3, 15, 0, 6, 10, 1, 13, 8, 9, 4, 5, 11, 12, 7, 2, 14 ]]; \s
             \s
                   /* Table - s5 */ \s
               var s5 = [ \s
                   [2, 12, 4, 1, 7, 10, 11, 6, 8, 5, 3, 15, 13, 0, 14, 9], \s
                   [14, 11, 2, 12, 4, 7, 13, 1, 5, 0, 15, 10, 3, 9, 8, 6], \s
                   [4, 2, 1, 11, 10, 13, 7, 8, 15, 9, 12, 5, 6, 3, 0, 14], \s
                   [11, 8, 12, 7, 1, 14, 2, 13, 6, 15, 0, 9, 10, 4, 5, 3 ]]; \s
             \s
                   /* Table - s6 */ \s
               var s6 = [ \s
                   [12, 1, 10, 15, 9, 2, 6, 8, 0, 13, 3, 4, 14, 7, 5, 11], \s
                   [10, 15, 4, 2, 7, 12, 9, 5, 6, 1, 13, 14, 0, 11, 3, 8], \s
                   [9, 14, 15, 5, 2, 8, 12, 3, 7, 0, 4, 10, 1, 13, 11, 6], \s
                   [4, 3, 2, 12, 9, 5, 15, 10, 11, 14, 1, 7, 6, 0, 8, 13 ]]; \s
             \s
                   /* Table - s7 */ \s
               var s7 = [ \s
                   [4, 11, 2, 14, 15, 0, 8, 13, 3, 12, 9, 7, 5, 10, 6, 1], \s
                   [13, 0, 11, 7, 4, 9, 1, 10, 14, 3, 5, 12, 2, 15, 8, 6], \s
                   [1, 4, 11, 13, 12, 3, 7, 14, 10, 15, 6, 8, 0, 5, 9, 2], \s
                   [6, 11, 13, 8, 1, 4, 10, 7, 9, 5, 0, 15, 14, 2, 3, 12]]; \s
             \s
                   /* Table - s8 */ \s
               var s8 = [ \s
                   [13, 2, 8, 4, 6, 15, 11, 1, 10, 9, 3, 14, 5, 0, 12, 7], \s
                   [1, 15, 13, 8, 10, 3, 7, 4, 12, 5, 6, 11, 0, 14, 9, 2], \s
                   [7, 11, 4, 1, 9, 12, 14, 2, 0, 6, 10, 13, 15, 3, 5, 8], \s
                   [2, 1, 14, 7, 4, 10, 8, 13, 15, 12, 9, 0, 3, 5, 6, 11]]; \s
                \s
               for(m=0;m<8;m++){ \s
               var i=0,j=0; \s
               i = expandByte[m*6+0]*2+expandByte[m*6+5]; \s
               j = expandByte[m * 6 + 1] * 2 * 2 * 2  \s
                 + expandByte[m * 6 + 2] * 2* 2  \s
                 + expandByte[m * 6 + 3] * 2  \s
                 + expandByte[m * 6 + 4]; \s
               switch (m) { \s
                 case 0 : \s
                   binary = getBoxBinary(s1[i][j]); \s
                   break; \s
                 case 1 : \s
                   binary = getBoxBinary(s2[i][j]); \s
                   break; \s
                 case 2 : \s
                   binary = getBoxBinary(s3[i][j]); \s
                   break; \s
                 case 3 : \s
                   binary = getBoxBinary(s4[i][j]); \s
                   break; \s
                 case 4 : \s
                   binary = getBoxBinary(s5[i][j]); \s
                   break; \s
                 case 5 : \s
                   binary = getBoxBinary(s6[i][j]); \s
                   break; \s
                 case 6 : \s
                   binary = getBoxBinary(s7[i][j]); \s
                   break; \s
                 case 7 : \s
                   binary = getBoxBinary(s8[i][j]); \s
                   break; \s
               }       \s
               sBoxByte[m*4+0] = parseInt(binary.substring(0,1)); \s
               sBoxByte[m*4+1] = parseInt(binary.substring(1,2)); \s
               sBoxByte[m*4+2] = parseInt(binary.substring(2,3)); \s
               sBoxByte[m*4+3] = parseInt(binary.substring(3,4)); \s
             } \s
             return sBoxByte; \s
            } \s
             \s
            function pPermute(sBoxByte){ \s
             var pBoxPermute = new Array(32); \s
             pBoxPermute[ 0] = sBoxByte[15];  \s
             pBoxPermute[ 1] = sBoxByte[ 6];  \s
             pBoxPermute[ 2] = sBoxByte[19];  \s
             pBoxPermute[ 3] = sBoxByte[20];  \s
             pBoxPermute[ 4] = sBoxByte[28];  \s
             pBoxPermute[ 5] = sBoxByte[11];  \s
             pBoxPermute[ 6] = sBoxByte[27];  \s
             pBoxPermute[ 7] = sBoxByte[16];  \s
             pBoxPermute[ 8] = sBoxByte[ 0];  \s
             pBoxPermute[ 9] = sBoxByte[14];  \s
             pBoxPermute[10] = sBoxByte[22];  \s
             pBoxPermute[11] = sBoxByte[25];  \s
             pBoxPermute[12] = sBoxByte[ 4];  \s
             pBoxPermute[13] = sBoxByte[17];  \s
             pBoxPermute[14] = sBoxByte[30];  \s
             pBoxPermute[15] = sBoxByte[ 9];  \s
             pBoxPermute[16] = sBoxByte[ 1];  \s
             pBoxPermute[17] = sBoxByte[ 7];  \s
             pBoxPermute[18] = sBoxByte[23];  \s
             pBoxPermute[19] = sBoxByte[13];  \s
             pBoxPermute[20] = sBoxByte[31];  \s
             pBoxPermute[21] = sBoxByte[26];  \s
             pBoxPermute[22] = sBoxByte[ 2];  \s
             pBoxPermute[23] = sBoxByte[ 8];  \s
             pBoxPermute[24] = sBoxByte[18];  \s
             pBoxPermute[25] = sBoxByte[12];  \s
             pBoxPermute[26] = sBoxByte[29];  \s
             pBoxPermute[27] = sBoxByte[ 5];  \s
             pBoxPermute[28] = sBoxByte[21];  \s
             pBoxPermute[29] = sBoxByte[10];  \s
             pBoxPermute[30] = sBoxByte[ 3];  \s
             pBoxPermute[31] = sBoxByte[24];     \s
             return pBoxPermute; \s
            } \s
             \s
            function finallyPermute(endByte){     \s
             var fpByte = new Array(64);   \s
             fpByte[ 0] = endByte[39];  \s
             fpByte[ 1] = endByte[ 7];  \s
             fpByte[ 2] = endByte[47];  \s
             fpByte[ 3] = endByte[15];  \s
             fpByte[ 4] = endByte[55];  \s
             fpByte[ 5] = endByte[23];  \s
             fpByte[ 6] = endByte[63];  \s
             fpByte[ 7] = endByte[31];  \s
             fpByte[ 8] = endByte[38];  \s
             fpByte[ 9] = endByte[ 6];  \s
             fpByte[10] = endByte[46];  \s
             fpByte[11] = endByte[14];  \s
             fpByte[12] = endByte[54];  \s
             fpByte[13] = endByte[22];  \s
             fpByte[14] = endByte[62];  \s
             fpByte[15] = endByte[30];  \s
             fpByte[16] = endByte[37];  \s
             fpByte[17] = endByte[ 5];  \s
             fpByte[18] = endByte[45];  \s
             fpByte[19] = endByte[13];  \s
             fpByte[20] = endByte[53];  \s
             fpByte[21] = endByte[21];  \s
             fpByte[22] = endByte[61];  \s
             fpByte[23] = endByte[29];  \s
             fpByte[24] = endByte[36];  \s
             fpByte[25] = endByte[ 4];  \s
             fpByte[26] = endByte[44];  \s
             fpByte[27] = endByte[12];  \s
             fpByte[28] = endByte[52];  \s
             fpByte[29] = endByte[20];  \s
             fpByte[30] = endByte[60];  \s
             fpByte[31] = endByte[28];  \s
             fpByte[32] = endByte[35];  \s
             fpByte[33] = endByte[ 3];  \s
             fpByte[34] = endByte[43];  \s
             fpByte[35] = endByte[11];  \s
             fpByte[36] = endByte[51];  \s
             fpByte[37] = endByte[19];  \s
             fpByte[38] = endByte[59];  \s
             fpByte[39] = endByte[27];  \s
             fpByte[40] = endByte[34];  \s
             fpByte[41] = endByte[ 2];  \s
             fpByte[42] = endByte[42];  \s
             fpByte[43] = endByte[10];  \s
             fpByte[44] = endByte[50];  \s
             fpByte[45] = endByte[18];  \s
             fpByte[46] = endByte[58];  \s
             fpByte[47] = endByte[26];  \s
             fpByte[48] = endByte[33];  \s
             fpByte[49] = endByte[ 1];  \s
             fpByte[50] = endByte[41];  \s
             fpByte[51] = endByte[ 9];  \s
             fpByte[52] = endByte[49];  \s
             fpByte[53] = endByte[17];  \s
             fpByte[54] = endByte[57];  \s
             fpByte[55] = endByte[25];  \s
             fpByte[56] = endByte[32];  \s
             fpByte[57] = endByte[ 0];  \s
             fpByte[58] = endByte[40];  \s
             fpByte[59] = endByte[ 8];  \s
             fpByte[60] = endByte[48];  \s
             fpByte[61] = endByte[16];  \s
             fpByte[62] = endByte[56];  \s
             fpByte[63] = endByte[24]; \s
             return fpByte; \s
            } \s
             \s
            function getBoxBinary(i) { \s
             var binary = ""; \s
             switch (i) { \s
               case 0 :binary = "0000";break; \s
               case 1 :binary = "0001";break; \s
               case 2 :binary = "0010";break; \s
               case 3 :binary = "0011";break; \s
               case 4 :binary = "0100";break; \s
               case 5 :binary = "0101";break; \s
               case 6 :binary = "0110";break; \s
               case 7 :binary = "0111";break; \s
               case 8 :binary = "1000";break; \s
               case 9 :binary = "1001";break; \s
               case 10 :binary = "1010";break; \s
               case 11 :binary = "1011";break; \s
               case 12 :binary = "1100";break; \s
               case 13 :binary = "1101";break; \s
               case 14 :binary = "1110";break; \s
               case 15 :binary = "1111";break; \s
             } \s
             return binary; \s
            } \s
            /*\s
            * generate 16 keys for xor\s
            *\s
            */ \s
            function generateKeys(keyByte){     \s
             var key   = new Array(56); \s
             var keys = new Array();   \s
              \s
             keys[ 0] = new Array(); \s
             keys[ 1] = new Array(); \s
             keys[ 2] = new Array(); \s
             keys[ 3] = new Array(); \s
             keys[ 4] = new Array(); \s
             keys[ 5] = new Array(); \s
             keys[ 6] = new Array(); \s
             keys[ 7] = new Array(); \s
             keys[ 8] = new Array(); \s
             keys[ 9] = new Array(); \s
             keys[10] = new Array(); \s
             keys[11] = new Array(); \s
             keys[12] = new Array(); \s
             keys[13] = new Array(); \s
             keys[14] = new Array(); \s
             keys[15] = new Array();   \s
             var loop = [1,1,2,2,2,2,2,2,1,2,2,2,2,2,2,1]; \s
             \s
             for(i=0;i<7;i++){ \s
               for(j=0,k=7;j<8;j++,k--){ \s
                 key[i*8+j]=keyByte[8*k+i]; \s
               } \s
             }     \s
              \s
             var i = 0; \s
             for(i = 0;i < 16;i ++){ \s
               var tempLeft=0; \s
               var tempRight=0; \s
               for(j = 0; j < loop[i];j ++){           \s
                 tempLeft = key[0]; \s
                 tempRight = key[28]; \s
                 for(k = 0;k < 27 ;k ++){ \s
                   key[k] = key[k+1]; \s
                   key[28+k] = key[29+k]; \s
                 }   \s
                 key[27]=tempLeft; \s
                 key[55]=tempRight; \s
               } \s
               var tempKey = new Array(48); \s
               tempKey[ 0] = key[13]; \s
               tempKey[ 1] = key[16]; \s
               tempKey[ 2] = key[10]; \s
               tempKey[ 3] = key[23]; \s
               tempKey[ 4] = key[ 0]; \s
               tempKey[ 5] = key[ 4]; \s
               tempKey[ 6] = key[ 2]; \s
               tempKey[ 7] = key[27]; \s
               tempKey[ 8] = key[14]; \s
               tempKey[ 9] = key[ 5]; \s
               tempKey[10] = key[20]; \s
               tempKey[11] = key[ 9]; \s
               tempKey[12] = key[22]; \s
               tempKey[13] = key[18]; \s
               tempKey[14] = key[11]; \s
               tempKey[15] = key[ 3]; \s
               tempKey[16] = key[25]; \s
               tempKey[17] = key[ 7]; \s
               tempKey[18] = key[15]; \s
               tempKey[19] = key[ 6]; \s
               tempKey[20] = key[26]; \s
               tempKey[21] = key[19]; \s
               tempKey[22] = key[12]; \s
               tempKey[23] = key[ 1]; \s
               tempKey[24] = key[40]; \s
               tempKey[25] = key[51]; \s
               tempKey[26] = key[30]; \s
               tempKey[27] = key[36]; \s
               tempKey[28] = key[46]; \s
               tempKey[29] = key[54]; \s
               tempKey[30] = key[29]; \s
               tempKey[31] = key[39]; \s
               tempKey[32] = key[50]; \s
               tempKey[33] = key[44]; \s
               tempKey[34] = key[32]; \s
               tempKey[35] = key[47]; \s
               tempKey[36] = key[43]; \s
               tempKey[37] = key[48]; \s
               tempKey[38] = key[38]; \s
               tempKey[39] = key[55]; \s
               tempKey[40] = key[33]; \s
               tempKey[41] = key[52]; \s
               tempKey[42] = key[45]; \s
               tempKey[43] = key[41]; \s
               tempKey[44] = key[49]; \s
               tempKey[45] = key[35]; \s
               tempKey[46] = key[28]; \s
               tempKey[47] = key[31]; \s
               switch(i){ \s
                 case 0: for(m=0;m < 48 ;m++){ keys[ 0][m] = tempKey[m]; } break; \s
                 case 1: for(m=0;m < 48 ;m++){ keys[ 1][m] = tempKey[m]; } break; \s
                 case 2: for(m=0;m < 48 ;m++){ keys[ 2][m] = tempKey[m]; } break; \s
                 case 3: for(m=0;m < 48 ;m++){ keys[ 3][m] = tempKey[m]; } break; \s
                 case 4: for(m=0;m < 48 ;m++){ keys[ 4][m] = tempKey[m]; } break; \s
                 case 5: for(m=0;m < 48 ;m++){ keys[ 5][m] = tempKey[m]; } break; \s
                 case 6: for(m=0;m < 48 ;m++){ keys[ 6][m] = tempKey[m]; } break; \s
                 case 7: for(m=0;m < 48 ;m++){ keys[ 7][m] = tempKey[m]; } break; \s
                 case 8: for(m=0;m < 48 ;m++){ keys[ 8][m] = tempKey[m]; } break; \s
                 case 9: for(m=0;m < 48 ;m++){ keys[ 9][m] = tempKey[m]; } break; \s
                 case 10: for(m=0;m < 48 ;m++){ keys[10][m] = tempKey[m]; } break; \s
                 case 11: for(m=0;m < 48 ;m++){ keys[11][m] = tempKey[m]; } break; \s
                 case 12: for(m=0;m < 48 ;m++){ keys[12][m] = tempKey[m]; } break; \s
                 case 13: for(m=0;m < 48 ;m++){ keys[13][m] = tempKey[m]; } break; \s
                 case 14: for(m=0;m < 48 ;m++){ keys[14][m] = tempKey[m]; } break; \s
                 case 15: for(m=0;m < 48 ;m++){ keys[15][m] = tempKey[m]; } break; \s
               } \s
             } \s
             return keys;   \s
            }
            """;
}
