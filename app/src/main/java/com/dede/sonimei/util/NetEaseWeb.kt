package com.dede.sonimei.util

import android.util.Base64

import java.io.UnsupportedEncodingException
import java.math.BigInteger
import java.net.URLEncoder
import java.util.Random

import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object NetEaseWeb {

    private const val nonce = "0CoJUm6Qyw8W8jud"
    private const val modulus = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7"
    private const val publicKey = "010001"

    // 加密
    private fun encrypt(sSrc: String?, sKey: String?): String? {
        if (sSrc == null || sKey == null) {
            return null
        }
        val skeySpec = SecretKeySpec(sKey.toByteArray(), "AES")
        try {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")//"算法/模式/补码方式"
            val iv = IvParameterSpec("0102030405060708".toByteArray())//使用CBC模式，需要一个向量iv，可增加加密算法的强度
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv)
            val encrypted = cipher.doFinal(sSrc.toByteArray())
            return Base64.encodeToString(encrypted, 0, encrypted.size, Base64.DEFAULT)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    fun netEaseWebRequestUrl(id: String): String {

        val jsonParams = "{\"csrf_token\": \"\", \"ids\": [$id], \"br\": 320000}"

        val randomStr = randomStr(16)
        println("randomStr: $randomStr")
        println("---------------------------")

        var encrypt = encrypt(jsonParams, nonce)// 第一次加密
        encrypt = encrypt(encrypt, randomStr)// 第二次加密
        var params: String? = null
        try {
            params = URLEncoder.encode(encrypt, "utf-8")
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        }

        println("params: " + params!!)

        val reverseRandomStr = StringBuilder(randomStr).reverse().toString()// 反转字符串
        val hexStr = parseByte2HexStr(reverseRandomStr.toByteArray())// 转16进制字符串


        val x = pow(hexStr, publicKey, modulus)
        val encSecKey = zfill(x, 256)
        println("encSecKey: $encSecKey")

        println("=========================")

        return String.format("http://music.163.com/weapi/song/enhance/player/url?params=%1s&encSecKey=%2s", params, encSecKey)
    }

    /**
     * 字符串右对齐，前面补0直到总长度为length，如果原本长度大于length就直接返回
     *
     * @param x      字符串
     * @param length 补0后的长度
     * @return
     */
    private fun zfill(x: String, length: Int): String {
        val l = x.length
        if (l < length) {
            val builder = StringBuilder()
            for (i in 0 until 256 - l) {
                builder.append("0")
            }
            return builder.append(x).toString()
        } else {
            return x
        }
    }

    /**
     * 快速幂算法 a^b%c
     *
     * @param aStr
     * @param bStr
     * @param zStr
     */
    private fun pow(aStr: String, bStr: String, zStr: String): String {
        var b = Integer.parseInt(bStr, 16)
        var a = BigInteger(aStr, 16)
        val c = BigInteger(zStr, 16)
        /*
         int PowerMod(int a, int b, int c)
         {
           int ans = 1;
           a = a % c;
           while (b > 0) {
               if(b % 2 == 1)
                   ans = (ans * a) % c;
               b = b / 2;
               a = (a * a) % c;
           }
           return ans;
         }
        */
        var ans = BigInteger.ONE
        a = a.remainder(c)
        while (b > 0) {
            if (b % 2 == 1) {
                ans = ans.multiply(a).remainder(c)
            }
            b /= 2
            a = a.pow(2).remainder(c)
        }
        return ans.toString(16)
    }

    /**
     * 将二进制转换成16进制字符串
     *
     * @param buf
     * @return
     */
    private fun parseByte2HexStr(buf: ByteArray): String {
        val sb = StringBuffer()
        for (i in buf.indices) {
            val hex = Integer.toHexString(buf[i].toInt())
            sb.append(hex.toUpperCase())
        }
        return sb.toString()
    }

    /**
     * 生成指定长度的随机字符串[a-zA-Z0-9]
     *
     * @param length
     * @return
     */
    private fun randomStr(length: Int): String {
        val all = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        val l = all.length
        val builder = StringBuilder()
        val random = Random()
        for (i in 0 until length) {
            builder.append(all[random.nextInt(l)])
        }
        return builder.toString()
    }
}