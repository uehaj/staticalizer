/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jggug.kobo.staticalizer

import static org.junit.Assert.*

/**
 * Utilities only for tests.
 */
class TestUtils {

    static executeCommandOk(args, closure = null) {
        def p = executeCommand(args, closure)
        if (p.exitValue() != 0) {
            fail "ERROR: exitValue:${p.exitValue()}, in:[${p.in.text}], err:[${p.err.text}]"
        }
        return p
    }

    static executeCommand(args, closure = null) {
        def command = getCommand(args)
        def p = command.execute()
        if (closure) closure.call(p)
        p.waitFor()
        //println "${command.join(' ')} => ${p.exitValue()}"
        return p
    }

    static executeCommandOkWithEnv(args, env, closure = null) {
        def p = executeCommandWithEnv(args, env, closure)
        if (p.exitValue() != 0) {
            fail "ERROR: exitValue:${p.exitValue()}, in:[${p.in.text}], err:[${p.err.text}]"
        }
        return p
    }

    static executeCommandWithEnv(args, env, closure = null) {
        def command = getCommandWithEnv(args, env)
        def p = command.execute()
        if (closure) closure.call(p)
        p.waitFor()
        //println "${command.join(' ')} => ${p.exitValue()}"
        return p
    }

    static getCommand(List args) {
        return args
    }

    /**
     * Get a command line which can invoke a command instance with specified
     * environment variable(s).
     */
    static getCommandWithEnv(args, env) {
        def result = ["env", *env, *args ]
        return result
    }

    /**
     * Reading and return available bytes.
     * This method is not blocking.
     */
    static getAvailableText(ins) {
        def byteList = new ArrayList<Byte>()
        int length
        while ((length = ins.available()) > 0) {
            byte[] bytes = new byte[length]
            int ret = ins.read(bytes, 0, length)
            if (ret == -1) {
                break
            }
            for (int i = 0; i < ret; i++) {
                byteList.add(bytes[i])
            }
        }
        return new String(byteList as byte[])
    }

}
