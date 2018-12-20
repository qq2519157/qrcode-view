package com.sencent.qrcodelib

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.zxing.DecodeHintType
import java.util.*
import java.util.regex.Pattern

/**
 *  Create by Logan at 2018/12/14 0014
 *
 */
object DecodeHintManager {

    private val TAG = DecodeHintManager::class.java.simpleName

    // This pattern is used in decoding integer arrays.
    private val COMMA = Pattern.compile(",")

    /**
     *
     * Split a query string into a list of name-value pairs.
     *
     *
     * This is an alternative to the [Uri.getQueryParameterNames] and
     * [Uri.getQueryParameters], which are quirky and not suitable
     * for exist-only Uri parameters.
     *
     *
     * This method ignores multiple parameters with the same name and returns the
     * first one only. This is technically incorrect, but should be acceptable due
     * to the method of processing Hints: no multiple values for a hint.
     *
     * @param query query to split
     * @return name-value pairs
     */
    private fun splitQuery(query: String): MutableMap<String, String> {
        val map = HashMap<String, String>()
        var pos = 0
        while (pos < query.length) {
            if (query[pos] == '&') {
                // Skip consecutive ampersand separators.
                pos++
                continue
            }
            val amp = query.indexOf('&', pos)
            val equ = query.indexOf('=', pos)
            if (amp < 0) {
                // This is the last element in the query, no more ampersand elements.
                var name: String
                var text: String
                if (equ < 0) {
                    // No equal sign
                    name = query.substring(pos)
                    name = name.replace('+', ' ') // Preemptively decode +
                    name = Uri.decode(name)
                    text = ""
                } else {
                    // Split name and text.
                    name = query.substring(pos, equ)
                    name = name.replace('+', ' ') // Preemptively decode +
                    name = Uri.decode(name)
                    text = query.substring(equ + 1)
                    text = text.replace('+', ' ') // Preemptively decode +
                    text = Uri.decode(text)
                }
                if (!map.containsKey(name)) {
                    map[name] = text
                }
                break
            }
            if (equ < 0 || equ > amp) {
                // No equal sign until the &: this is a simple parameter with no value.
                var name = query.substring(pos, amp)
                name = name.replace('+', ' ') // Preemptively decode +
                name = Uri.decode(name)
                if (!map.containsKey(name)) {
                    map[name] = ""
                }
                pos = amp + 1
                continue
            }
            var name = query.substring(pos, equ)
            name = name.replace('+', ' ') // Preemptively decode +
            name = Uri.decode(name)
            var text = query.substring(equ + 1, amp)
            text = text.replace('+', ' ') // Preemptively decode +
            text = Uri.decode(text)
            if (!map.containsKey(name)) {
                map[name] = text
            }
            pos = amp + 1
        }
        return map
    }

    internal fun parseDecodeHints(inputUri: Uri): MutableMap<DecodeHintType, Any>? {
        val query = inputUri.encodedQuery
        if (query == null || query.isEmpty()) {
            return null
        }
        // Extract parameters
        val parameters = splitQuery(query)
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
        for (hintType in DecodeHintType.values()) {
            if (hintType == DecodeHintType.CHARACTER_SET ||
                hintType == DecodeHintType.NEED_RESULT_POINT_CALLBACK ||
                hintType == DecodeHintType.POSSIBLE_FORMATS
            ) {
                continue // This hint is specified in another way
            }
            val parameterName = hintType.name
            var parameterText: String? = parameters[parameterName] ?: continue
            if (hintType.valueType == Any::class.java) {
                // This is an unspecified type of hint content. Use the value as is.
                // TODO: Can we make a different assumption on this?
                hints[hintType] = parameterText
                continue
            }
            if (hintType.valueType == Void::class.java) {
                // Void hints are just flags: use the constant specified by DecodeHintType
                hints[hintType] = java.lang.Boolean.TRUE
                continue
            }
            if (hintType.valueType == String::class.java) {
                // A string hint: use the decoded value.
                hints[hintType] = parameterText
                continue
            }
            if (hintType.valueType == Boolean::class.java) {
                // A boolean hint: a few values for false, everything else is true.
                // An empty parameter is simply a flag-style parameter, assuming true
                if (parameterText!!.isEmpty()) {
                    hints[hintType] = java.lang.Boolean.TRUE
                } else if ("0" == parameterText ||
                    "false".equals(parameterText, ignoreCase = true) ||
                    "no".equals(parameterText, ignoreCase = true)
                ) {
                    hints[hintType] = java.lang.Boolean.FALSE
                } else {
                    hints[hintType] = java.lang.Boolean.TRUE
                }
                continue
            }
            if (hintType.valueType == IntArray::class.java) {
                // An integer array. Used to specify valid lengths.
                // Strip a trailing comma as in Java style array initialisers.
                if (!parameterText!!.isEmpty() && parameterText[parameterText.length - 1] == ',') {
                    parameterText = parameterText.substring(0, parameterText.length - 1)
                }
                val values = COMMA.split(parameterText)
                var array: IntArray? = IntArray(values.size)
                for (i in values.indices) {
                    try {
                        array!![i] = Integer.parseInt(values[i])
                    } catch (ignored: NumberFormatException) {
                        Log.w(
                            TAG,
                            "Skipping array of integers hint " + hintType + " due to invalid numeric value: '" + values[i] + '\''.toString()
                        )
                        array = null
                        break
                    }
                }
                if (array != null) {
                    hints[hintType] = array
                }
                continue
            }
            Log.w(TAG, "Unsupported hint type '" + hintType + "' of type " + hintType.valueType)
        }
        Log.i(TAG, "Hints from the URI: $hints")
        return hints
    }

    internal fun parseDecodeHints(intent: Intent): MutableMap<DecodeHintType, Any>? {
        val extras = intent.extras
        if (extras == null || extras.isEmpty) {
            return null
        }
        val hints = EnumMap<DecodeHintType, Any>(DecodeHintType::class.java)
        for (hintType in DecodeHintType.values()) {
            if (hintType == DecodeHintType.CHARACTER_SET ||
                hintType == DecodeHintType.NEED_RESULT_POINT_CALLBACK ||
                hintType == DecodeHintType.POSSIBLE_FORMATS
            ) {
                continue // This hint is specified in another way
            }
            val hintName = hintType.name
            if (extras.containsKey(hintName)) {
                if (hintType.valueType == Void::class.java) {
                    // Void hints are just flags: use the constant specified by the DecodeHintType
                    hints[hintType] = java.lang.Boolean.TRUE
                } else {
                    val hintData = extras.get(hintName)
                    if (hintType.valueType.isInstance(hintData)) {
                        hints[hintType] = hintData
                    } else {
                        Log.w(TAG, "Ignoring hint $hintType because it is not assignable from $hintData")
                    }
                }
            }
        }
        Log.i(TAG, "Hints from the Intent: $hints")
        return hints
    }
}