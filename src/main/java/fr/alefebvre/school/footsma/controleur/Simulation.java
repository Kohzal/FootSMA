/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Arthur Lefebvre
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */

package fr.alefebvre.school.footsma.controleur;

import fr.alefebvre.school.footsma.modele.*;
import fr.alefebvre.school.footsma.vue.SimulationWindow;
import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.ContainerController;
import jade.wrapper.StaleProxyException;

import java.awt.*;
import java.awt.image.BufferStrategy;

public class Simulation extends Canvas implements Runnable {

    public static final long serialVersionUID = 1L;

    private boolean running = false;
    private Thread thread;
    private AgentHandler agentHandler;

    public Simulation(AgentContainer agentContainer) throws StaleProxyException {
        new SimulationWindow(Constants.WIDTH, Constants.HEIGHT, "Football simulation", this);
        agentHandler = new AgentHandler();
        initialize(agentContainer);
    }

    public static void main(String[] args) {
        // Get a hold on JADE runtime
        jade.core.Runtime rt = Runtime.instance();
        /*
            http://jade.tilab.com/pipermail/jade-develop/2008q3/012874.html
        */
        rt.setCloseVM(true);
        System.out.print("runtime created\n");
        // Create a default profile
        Profile profile = new ProfileImpl();
        System.out.print("profile created\n");

        System.out.println("Launching a whole in-process platform..." + profile);
        jade.wrapper.AgentContainer mainContainer = rt.createMainContainer(profile);

        // now set the default Profile to start a container
        ProfileImpl pContainer = new ProfileImpl(null, 1200, null);
        System.out.println("Launching the agent container ..." + pContainer);

        jade.wrapper.AgentContainer agentContainer = rt.createAgentContainer(pContainer);
        System.out.println("Launching the agent container after ..." + pContainer);

        System.out.println("containers created");
        System.out.println("Launching the rma agent on the main container ...");
        AgentController rma;
        try {
            rma = mainContainer.createNewAgent("rma",
                    "jade.tools.rma.rma", new Object[0]);
            rma.start();
            new Simulation(agentContainer);
        } catch (StaleProxyException e) {
            e.printStackTrace();
        }
    }

    public static Object[] getArgsJoueur(AgentController terrain, AgentHandler agentHandler, boolean estGardien, int numEquipe, int numJoueur) {
        Object[] argsJoueur = new Object[6];
        argsJoueur[0] = agentHandler;
        // couleur
        if (estGardien)
            argsJoueur[1] = ReglesDuJeu.COULEUR_GARDIENS;
        else if (numEquipe == 1)
            argsJoueur[1] = ReglesDuJeu.COULEUR_EQUIPE_1;
        else
            argsJoueur[1] = ReglesDuJeu.COULEUR_EQUIPE_2;
        if (estGardien) {
            if (numEquipe == 1)
                argsJoueur[2] = new Position(ReglesDuJeu.BUT_EQUIPE_1);
            else
                argsJoueur[2] = new Position(ReglesDuJeu.BUT_EQUIPE_2);
        } else {
            if (numEquipe == 1)
                argsJoueur[2] = PositionHelper.milieu(ReglesDuJeu.BUT_EQUIPE_1, ReglesDuJeu.MILIEU_DE_TERRAIN);
            else
                argsJoueur[2] = PositionHelper.milieu(ReglesDuJeu.BUT_EQUIPE_2, ReglesDuJeu.MILIEU_DE_TERRAIN);
        }

        argsJoueur[3] = numJoueur;
        argsJoueur[4] = estGardien;
        argsJoueur[5] = numEquipe;
        //argsJoueur[6] = terrain;
        return argsJoueur;
    }

    public void initialize(AgentContainer agentContainer) throws StaleProxyException {

        agentHandler.getObjects().clear();
        Object[] argsT = new Object[1];
        argsT[0] = agentHandler;
        AgentController terrain = agentContainer.createNewAgent("terrain",
                AgentTerrain.class.getName(), argsT);
        terrain.start();

        // Gardiens
        Object[] argsJoueur1 = getArgsJoueur(terrain, agentHandler, true, 1, 1);
        Object[] argsJoueur2 = getArgsJoueur(terrain, agentHandler, true, 2, 1);

        // Creation des joueurs
        Object[] argsJoueur3 = getArgsJoueur(terrain, agentHandler, false, 1, 9);
        Object[] argsJoueur4 = getArgsJoueur(terrain, agentHandler, false, 2, 10);

        AgentController joueur1 = agentContainer.createNewAgent("joueur1", AgentJoueur.class.getName(),
                argsJoueur1);
        AgentController joueur2 = agentContainer.createNewAgent("joueur2",
                AgentJoueur.class.getName(), argsJoueur2);
        AgentController joueur3 = agentContainer.createNewAgent("joueur3", AgentJoueur.class.getName(),
                argsJoueur3);
        AgentController joueur4 = agentContainer.createNewAgent("joueur4",
                AgentJoueur.class.getName(), argsJoueur4);


        joueur1.start();
        joueur2.start();
        joueur3.start();
        joueur4.start();

        Object[] argsArbitre = new Object[2];
        argsArbitre[0] = 0;
        argsArbitre[1] = agentHandler;
        AgentController arbitre = agentContainer.createNewAgent("Arbitre", AgentArbitre.class.getName(), argsArbitre);
        arbitre.start();

        //agentHandler.addMap(new TilesMap(0, 0, Constants.TEST_MAP_PATH));
        // agentHandler.getObjects().add(new Player(0, 0, 0, 0, agentHandler));
    }

    public synchronized void start() {
        if (running)
            return;
        running = true;
        thread = new Thread(this);
        thread.start();

    }

    synchronized void stop() {
        if (!running)
            return;
        running = false;
        try {
            thread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.exit(1);
    }

    public void run() {
        this.requestFocus();
        //long lastTime = System.nanoTime();
        // final double amountOfTicks = 60.0;
        // double ns = 1000000000 / amountOfTicks;
        //double delta = 0;
        int frames = 0;
        long timer = System.currentTimeMillis();
        while (running) {
            //long now = System.nanoTime();
            //delta += (now - lastTime) / ns;
            //lastTime = now;
            /*if (delta >= 1) {
                tick();
                delta--;
            }*/
            if (running)
                render();
            frames++;

            if (System.currentTimeMillis() - timer > 1000) {
                timer += 1000;
                System.out.println("FPS " + frames);
                frames = 0;
            }
        }
        stop();
    }

/*    private void tick() {
        this.agentHandler.tick();
    }*/

    private void render() {
        BufferStrategy bs = this.getBufferStrategy();
        if (bs == null) {
            this.createBufferStrategy(3);
            return;
        }
        Graphics g = bs.getDrawGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, Constants.WIDTH, Constants.HEIGHT);
        this.agentHandler.render(g);
        g.dispose();
        bs.show();
    }

    public void startMatch() {
        if (agentHandler != null) {
            agentHandler.startMatch();
        }
    }

    public void endMatch() {
        //stop();
        System.exit(0);
    }
}
