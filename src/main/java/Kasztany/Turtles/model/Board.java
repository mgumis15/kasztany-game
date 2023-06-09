package Kasztany.Turtles.model;

import Kasztany.Turtles.model.cards.*;
import Kasztany.Turtles.parser.MapParser;
import Kasztany.Turtles.persistence.GameLog;
import Kasztany.Turtles.persistence.GameLogRepository;
import Kasztany.Turtles.settings.GlobalSettings;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;


import java.io.*;
import java.util.*;

@Component
@Scope("prototype")
public class Board {
    private final Neighbourhood neighbourhood;
    private final ArrayList<Turtle> turtles;
    private Vector2d maxVector = new Vector2d();
    private Field lastField;
    private Field startField;
    private final GameLogRepository gameLogRepository;
    private final ArrayList<Card> availableCards = new ArrayList<>();
    private final ArrayList<Card> usedCards = new ArrayList<>();
    private final Turn turn;


    public Board(GameLogRepository repository) {
        this.gameLogRepository = repository;
        this.neighbourhood = new Neighbourhood();
        this.turtles = new ArrayList<>();
        this.turn = new Turn(turtles);
    }

    public void createCards(int total, ArrayList<String> availableColors) {
        for (int i = 0; i < total; i++) {
            int number = GlobalSettings.getRandomNumber(0, 5);
            if (number == 0)
                availableCards.add(new ChoosedMoveCard(this));
            if (number == 1)
                availableCards.add(new ColorBasedMoveCard(this, availableColors));
            if (number == 2)
                availableCards.add(new LastTurtleMoveCard(this));
            if (number == 3)
                availableCards.add(new MoveTurtleInStackCard(this, availableColors));
            if (number == 4)
                availableCards.add(new SwapTurtlesInStackCard(this));
        }
//        Card card1 = new ChoosedMoveCard(this, 2, true);
//        Card card2 = new ColorBasedMoveCard(this, 2, true, "red");
//        Card card3 = new LastTurtleMoveCard(this, 2);
//        Card card4 = new MoveTurtleInStackCard(this, true);
//        Card card5 = new MoveTurtleInStackCard(this, false);
//        Card card6 = new SwapTurtlesInStackCard(this);
//        availableCards.addAll(List.of(card1, card2, card3, card4, card5, card6));
    }

    public void changeTurn() {
        turn.next();
    }

    public Player getCurrentPlayer() {
        return turn.getCurrentPlayer();
    }

    public ArrayList<Card> getAvailableCards() {
        return availableCards;
    }

    public void addFields(File map) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(map));
        MapParser mapParser = new MapParser();

        Field startField = mapParser.parseMapLine(bufferedReader.readLine());
        maxVector = maxVector.setMaximal(startField.getPosition());
        neighbourhood.addField(startField.getPosition(), startField);
        this.startField = startField;

        bufferedReader.lines().forEach(line -> {
            System.out.println(line);
            Field field = mapParser.parseMapLine(line);
            if (field.getPossibleForwardDirections().isEmpty()) {
                this.lastField = field;
            }
            maxVector = maxVector.setMaximal(field.getPosition());
            neighbourhood.addField(field.getPosition(), field);
        });

    }

    public void addTurtlesFromHashMap(HashMap<Integer, List<String>> players) {
        Vector2d startVector = startField.getPosition();
        for (int key : players.keySet()) {
            turtles.add(new Turtle(players.get(key).get(0), players.get(key).get(1), neighbourhood.getFieldByVector(startVector)));
        }
        ArrayList<String> availableColors = new ArrayList<>();
        for (Turtle turtle : turtles) {
            startField.addTurtle(turtle);
            availableColors.add(turtle.getColor());
        }
        createCards(turtles.size() * 5 + 1, availableColors);
        handCardsToPlayers();
    }

    private void handCardsToPlayers() {
        for (int i = turtles.size() * 5 - 1; i >= 0; i--) {
            Turtle turtle = turtles.get(i % turtles.size());
            turtle.addPlayerCard(availableCards.remove(i));
        }
    }

    public Neighbourhood getNeighbourhood() {
        return neighbourhood;
    }

    public Field getStartingField() {
        return startField;
    }

    public Field getLastField() {
        return lastField;
    }

    public Vector2d getMaxVector() {
        return maxVector;
    }

    public ArrayList<Turtle> getTurtles() {
        return turtles;
    }

    public Field getFieldForTurtleMove(Vector2d turtlePosition, Direction direction) {
        Vector2d nextTurtlePosition = turtlePosition.add(direction.toVector());
        return neighbourhood.getFieldByVector(nextTurtlePosition);
    }

    public GameLogRepository getGameLogRepository() {
        return gameLogRepository;
    }

    public void saveGameLog(int winnerIndex, int secondIndex, int thirdIndex) {
        Turtle winner = turtles.get(winnerIndex);
        GameLog gameLog = new GameLog(turtles.size(), neighbourhood.getWholeNeighbourhood().size(), winner.getName(), winner.getPoints());
        Turtle second, third;
        if(secondIndex >= 0){
            second = turtles.get(secondIndex);
            gameLog.setSecondPlayer(second.getName(), second.getPoints());
        }

        if(thirdIndex >= 0){
            third = turtles.get(thirdIndex);
            gameLog.setThirdPlayer(third.getName(), third.getPoints());
        }
        gameLogRepository.save(gameLog);
    }

    public Turtle findWinner() {
        int winnerPoints = turtles.size() * 5;

        Turtle currTurtle = lastField.getTopTurtle().orElse(null);
        while (currTurtle != null) {
            currTurtle.addPoints(winnerPoints);
            winnerPoints -= 5;
            currTurtle = currTurtle.getTurtleOnBottom().orElse(null);
        }

        Turtle winningTurtle = this.turtles.get(0);
        for (Turtle turtle : this.turtles) {
            if (turtle.getPoints() > winningTurtle.getPoints()) {
                winningTurtle = turtle;
            }
        }

        this.turtles.sort(Comparator.comparing(Turtle::getPoints));
        Collections.reverse(this.turtles);

        if(this.turtles.size() > 1){
            if(this.turtles.size() > 2){
                saveGameLog(0, 1, 2);
            }else{
                saveGameLog(0, 1, -1);
            }
        }else{
            saveGameLog(0, -1, -1);
        }
        return winningTurtle;
    }

    public Boolean isGameEnd() {
        return lastField.hasTurtle();
    }

    public Optional<Turtle> getTurtleWithColor(String color){
        for(Turtle turtle: this.turtles){
            if(turtle.getColor().equals(color)){
                return Optional.of(turtle);
            }
        }
        return Optional.empty();
    }

    public void burnCard(Card choosedCard) {
        Player currentPlayer = getCurrentPlayer();
        currentPlayer.removeCard(choosedCard);
        if(availableCards.size() == 0){
            ArrayList<String> colors = new ArrayList<>();
            for(Turtle turtle: turtles){
                colors.add(turtle.getColor());
            }
            createCards(30, colors);
        }
        currentPlayer.addCard(availableCards.remove(0));
    }
}
