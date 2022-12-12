package Kasztany.Turtles.model;

import java.util.Optional;

public class Field {
    private final int id;
    private final Vector2d position;
    private Optional<Turtle> turtle;
    private Optional<Fruit> fruit;

    public Field(Integer id, Vector2d position) {
        this.id = id;
        this.position = position;
        this.turtle = Optional.empty();
        this.fruit = Optional.empty();
    }

    public Vector2d getPosition() {
        return position;
    }

    public void linkTurtle(Turtle turtle) {
        this.turtle = Optional.of(turtle);
    }

    public void freeField() {
        turtle = Optional.empty();
    }

    public void addFruit(int points){
        fruit = Optional.of(new Fruit(points));
    }

    public Optional<Fruit> getFruit() {
        return fruit;
    }

    public void deleteFruit(){
        fruit = Optional.empty();
    }

    public int getTurtlesNumber() {
        int turtles = 0;
        if (this.turtle.isEmpty()) {
            return turtles;
        }
        Turtle loopTurtle = this.turtle.get();
        turtles++;
        while (loopTurtle.getTurtleOnBack().isPresent()) {
            turtles++;
            loopTurtle = loopTurtle.getTurtleOnBack().get();
        }
        return turtles;
    }

    public Optional<Turtle> getBottomTurtle() {
        return turtle;
    }

    public Optional<Turtle> getTopTurtle() {
        if (this.turtle.isEmpty()) {
            return Optional.empty();
        }
        Turtle loopTurtle = this.turtle.get();
        while (loopTurtle.getTurtleOnBack().isPresent()) {
            loopTurtle = loopTurtle.getTurtleOnBack().get();
        }
        return Optional.of(loopTurtle);
    }

    public Boolean hasTurtle() {
        return this.turtle.isPresent();
    }
}
