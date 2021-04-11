/*
 *  Copyright (c) 2021. Linkit and or its affiliates. All rights reserved.
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 *  This code is free software; you can only use it for personal uses, studies or documentation.
 *  You can download this source code, and modify it ONLY FOR PERSONAL USE and you
 *  ARE NOT ALLOWED to distribute your MODIFIED VERSION.
 *
 *  Please contact maximebatista18@gmail.com if you need additional information or have any
 *  questions.
 */

package fr.linkit.prototypes;

import fr.linkit.core.connection.network.cache.puppet.Puppeteer;
import fr.linkit.plugin.debug.commands.PuppetCommand;

import java.util.Arrays;

public class JavaTests {

    //This class is a hand made generated class like in order to visualize
    //how a generated class using PuppetClassGenerator looks like.
    class PuppetPlayer extends PuppetCommand.Player {

        private final Puppeteer puppeteer;
        private String getName_0;

        public PuppetPlayer(Puppeteer $1, PuppetCommand.Player $2) {
            super($2);
            this.puppeteer = $1;
            this.puppeteer.init(this);
        }

        @Override
        public String getName() {
            if (getName_0 == null) {
                getName_0 = puppeteer.sendInvokeAndReturn("getName", new Object[]{});
            }
            return getName_0;
        }
    }

}
