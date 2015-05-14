package fr.alefebvre.school.footsma.modele;

import fr.alefebvre.school.footsma.controleur.AgentHandler;
import fr.alefebvre.school.footsma.controleur.GameObject;
import jade.core.AID;
import jade.core.behaviours.CyclicBehaviour;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class AgentTerrain extends GameObject {
    private AgentHandler handler;
    private Position ballonPos = new Position(ReglesDuJeu.getPosMillieuTerrain());
    private boolean possessionEquipe1;
    private boolean possessionEquipe2;
    private Position posJoueurAuBallon;

    public Position getBallonPos() {
        return ballonPos;
    }

    public void setBallonPos(Position ballonPos) {
        this.ballonPos = ballonPos;
    }

    public boolean isBallonDisponible() {
        return ballonDisponible;
    }

    public void setBallonDisponible(boolean ballonDisponible) {
        this.ballonDisponible = ballonDisponible;
    }

    public AID getJoueurAuBallon() {
        return joueurAuBallon;
    }

    public void setJoueurAuBallon(AID joueurAuBallon) {
        this.joueurAuBallon = joueurAuBallon;
    }

    private boolean ballonDisponible = true;
    private AID joueurAuBallon;

    private MessageTemplate template = MessageTemplate.and(
            MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF),
            MessageTemplate.MatchOntology("ballon"));

    protected void setup() {
        Object[] args = getArguments();
        handler = (AgentHandler) args[0];
        handler.getObjects().add(this);
        handler.setTerrainId(this.getAID());
        handler.setTerrain(this);

        System.out.println("Agent" + getLocalName() + " est créé");
        // Make this agent terminate
        addBehaviour(new CyclicBehaviour(this) {
            public void action() {
                ACLMessage msg = myAgent.receive(template);
                if (msg != null) {
                    System.out.println("Received QUERY_IF message from agent " + msg.getSender().getName());
                    ACLMessage reply = msg.createReply();
                    reply.setOntology("ballon");
                    if ("BALLONDISPO".equals(msg.getContent())) {
                        if (ballonDisponible) {
                            reply.setPerformative(ACLMessage.INFORM);
                            reply.setContent("dispo");
                            ballonDisponible = false;
                            joueurAuBallon = msg.getSender();
                        } else {
                            reply.setPerformative(ACLMessage.INFORM);
                            reply.setContent(joueurAuBallon.getLocalName());
                        }
                    } else {
                        reply.setPerformative(ACLMessage.NOT_UNDERSTOOD);
                        reply.setContent("Unknown-content");
                    }
                    myAgent.send(reply);
                } else {
                    block();
                }
            }
        });
    }

    public void doDelete() {
        super.doDelete();
    }

    protected void takeDown() {
        System.out.println("Agent " + getLocalName() + ": terminating");
    }

    @Override
    public void render(Graphics g) {
        try {
            BufferedImage imgTerrain = ImageIO.read(new File("src/main/resources/images/terrain.jpg"));
            g.drawImage(imgTerrain, 0, 0, null);
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Pb de chargement d'image");
        }
        g.setColor(Color.WHITE);
        g.fillOval(ballonPos.getX(), ballonPos.getY(), 12, 12);
        g.setColor(Color.BLACK);
        g.drawOval(ballonPos.getX(), ballonPos.getY(), 12, 12);
        g.fillOval(ballonPos.getX(), ballonPos.getY() - 40, 20, 20);
        g.setColor(Color.WHITE);
        g.drawOval(ballonPos.getX(), ballonPos.getY() - 40, 20, 20);
    }

    public boolean isPossessionEquipe1() {
        return possessionEquipe1;
    }

    public boolean isPossessionEquipe2() {
        return possessionEquipe2;
    }

    public void setPossession(int numero) {
        if (numero == 1) {
            setPossessionEquipe1(true);
            setPossessionEquipe2(false);
        } else if (numero == 2) {
            setPossessionEquipe2(true);
            setPossessionEquipe1(false);
        } else {
            setPossessionEquipe1(false);
            setPossessionEquipe2(false);
        }
    }

    public void setPossessionEquipe1(boolean possessionEquipe1) {
        this.possessionEquipe1 = possessionEquipe1;
    }

    public void setPossessionEquipe2(boolean possessionEquipe2) {
        this.possessionEquipe2 = possessionEquipe2;
    }

    public void setPosJoueurAuBallon(Position posJoueurAuBallon) {
        this.posJoueurAuBallon = posJoueurAuBallon;
    }
}
