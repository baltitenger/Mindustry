package io.anuke.mindustry.world.blocks.power;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntSet;
import com.badlogic.gdx.utils.ObjectSet;
import com.badlogic.gdx.utils.Queue;
import io.anuke.mindustry.world.Tile;
import io.anuke.mindustry.world.consumers.Consume;
import io.anuke.mindustry.world.consumers.ConsumePower;
import io.anuke.mindustry.world.consumers.Consumers;
import io.anuke.ucore.util.Mathf;

import java.util.Locale;

import static io.anuke.mindustry.Vars.threads;

public class PowerGraph{
    private final static Queue<Tile> queue = new Queue<>();
    private final static Array<Tile> outArray1 = new Array<>();
    private final static Array<Tile> outArray2 = new Array<>();
    private final static IntSet closedSet = new IntSet();

    private final ObjectSet<Tile> producers = new ObjectSet<>();
    private final ObjectSet<Tile> consumers = new ObjectSet<>();
    private final ObjectSet<Tile> batteries = new ObjectSet<>();
    private final ObjectSet<Tile> all = new ObjectSet<>();

    private float powerProduced = 0f;
    private float powerNeeded = 0f;
    private float totalAccumulator = 0f;
    private float totalCapacity = 0f;
    private float batteryCharged = 0f;

    private float powerUsed = 0f;
    private float powerChanged = 0f;
    
    public final PowerInfo info = new PowerInfo(this);
    public class PowerInfo{
        public final PowerGraph graph;

        private PowerInfo(PowerGraph graph){
            this.graph = graph;
        }

        public float getPowerProduced(){
            return graph.powerProduced;
        }

        public float getPowerNeeded(){
            return graph.powerNeeded;
        }

        public float getBatteryStored(){
            return graph.totalAccumulator;
        }

        public float getBatteryCapacity(){
            return graph.totalCapacity;
        }

        public float getBatteryCharged(){
            return graph.batteryCharged;
        }

        public float getPowerUsed(){
            return graph.powerUsed;
        }

        public float getPowerChanged(){
            return graph.powerChanged;
        }

        @Override
        public String toString(){
            return String.format(Locale.ROOT, "PowerInfo{powerProduced=%f, powerNeeded=%f, batteryStored=%f, batteryCapacity=%f, batteryCharged=%f, powerUsed=%f, powerChanged=%f}",
                    getPowerProduced(), getPowerNeeded(), getBatteryStored(), getBatteryCapacity(), getBatteryCharged(), getPowerUsed(), getPowerChanged());
        }
    }

    private long lastFrameUpdated;
    private final int graphID;
    private static int lastGraphID;

    {
        graphID = lastGraphID++;
    }

    public int getID(){
        return graphID;
    }

    public float getPowerProduced(){
        powerProduced = 0f;
        for(Tile producer : producers){
            powerProduced += producer.block().getPowerProduction(producer) * producer.entity.delta();
        }
        return powerProduced;
    }

    public float getPowerNeeded(){
        powerNeeded = 0f;
        for(Tile consumer : consumers){
            Consumers consumes = consumer.block().consumes;
            if(consumes.hasSubtypeOf(ConsumePower.class)){
                ConsumePower consumePower = consumes.getSubtypeOf(ConsumePower.class);
                if(otherConsumersAreValid(consumer, consumePower)){
                    powerNeeded += consumePower.requestedPower(consumer.block(), consumer.entity) * consumer.entity.delta();
                }
            }
        }
        return powerNeeded;
    }

    public float getBatteryStored(){
        totalAccumulator = 0f;
        for(Tile battery : batteries){
            Consumers consumes = battery.block().consumes;
            if(consumes.hasSubtypeOf(ConsumePower.class)){
                totalAccumulator += battery.entity.power.satisfaction * consumes.getSubtypeOf(ConsumePower.class).powerCapacity;
            }
        }
        return totalAccumulator;
    }

    public float getBatteryCapacity(){
        totalCapacity = 0f;
        for(Tile battery : batteries){
            Consumers consumes = battery.block().consumes;
            if(consumes.hasSubtypeOf(ConsumePower.class)){
                totalCapacity += consumes.getSubtypeOf(ConsumePower.class).requestedPower(battery.block(), battery.entity) * battery.entity.delta();
            }
        }
        return totalCapacity;
    }

    public float useBatteries(float needed){
        float stored = getBatteryStored();
        if(MathUtils.isEqual(stored, 0f)){ return 0f; }

        float used = Math.min(stored, needed);
        float consumedPowerPercentage = Math.min(1.0f, needed / stored);
        for(Tile battery : batteries){
            Consumers consumes = battery.block().consumes;
            if(consumes.hasSubtypeOf(ConsumePower.class)){
                ConsumePower consumePower = consumes.getSubtypeOf(ConsumePower.class);
                if(consumePower.powerCapacity > 0f){
                    battery.entity.power.satisfaction = Math.max(0.0f, battery.entity.power.satisfaction - consumedPowerPercentage);
                }
            }
        }
        return used;
    }

    public float chargeBatteries(float excess){
        float capacity = getBatteryCapacity();
        if(MathUtils.isEqual(capacity, 0f)){ return 0f; }

        for(Tile battery : batteries){
            Consumers consumes = battery.block().consumes;
            if(consumes.hasSubtypeOf(ConsumePower.class)){
                ConsumePower consumePower = consumes.getSubtypeOf(ConsumePower.class);
                if(consumePower.powerCapacity > 0f){
                    float additionalPowerPercentage = Math.min(1.0f, excess / consumePower.powerCapacity);
                    battery.entity.power.satisfaction = Math.min(1.0f, battery.entity.power.satisfaction + additionalPowerPercentage);
                }
            }
        }
        return batteryCharged = Math.min(excess, capacity);
    }

    public void distributePower(float needed, float produced){
        if(MathUtils.isEqual(needed, 0f)){ return; }

        float coverage = Math.min(1, produced / needed);
        for(Tile consumer : consumers){
            Consumers consumes = consumer.block().consumes;
            if(consumes.hasSubtypeOf(ConsumePower.class)){
                ConsumePower consumePower = consumes.getSubtypeOf(ConsumePower.class);
                if(!otherConsumersAreValid(consumer, consumePower)){
                    consumer.entity.power.satisfaction = 0.0f; // Only supply power if the consumer would get valid that way
                }else{
                    if(consumePower.isBuffered){
                        // Add an equal percentage of power to all buffers, based on the global power coverage in this graph
                        float maximumRate = consumePower.requestedPower(consumer.block(), consumer.entity()) * coverage * consumer.entity.delta();
                        consumer.entity.power.satisfaction = Mathf.clamp(consumer.entity.power.satisfaction + maximumRate / consumePower.powerCapacity);
                    }else{
                        consumer.entity.power.satisfaction = coverage;
                    }
                }
            }
        }
    }

    public void update(){
        if(threads.getFrameID() == lastFrameUpdated || consumers.size == 0 && producers.size == 0 && batteries.size == 0) return;

        float powerNeeded = getPowerNeeded();
        float powerProduced = getPowerProduced();
        powerChanged = powerProduced - powerNeeded;

        if(!MathUtils.isEqual(powerNeeded, powerProduced)){
            if(powerNeeded > powerProduced){
                powerProduced += useBatteries(powerNeeded - powerProduced);
            }else if(powerProduced > powerNeeded){
                powerProduced -= chargeBatteries(powerProduced - powerNeeded);
            }
        }
        powerUsed = Math.min(powerNeeded, powerProduced);

        distributePower(powerNeeded, powerProduced);

        lastFrameUpdated = threads.getFrameID();
    }

    public void add(PowerGraph graph){
        for(Tile tile : graph.all){
            add(tile);
        }
    }

    public void add(Tile tile){
        tile.entity.power.graph = this;
        all.add(tile);

        if(tile.block().outputsPower && tile.block().consumesPower){
            batteries.add(tile);
        }else if(tile.block().outputsPower){
            producers.add(tile);
        }else if(tile.block().consumesPower){
            consumers.add(tile);
        }
    }

    public void clear(){
        for(Tile other : all){
            if(other.entity != null && other.entity.power != null){
                other.entity.power.satisfaction = 0.0f;
                other.entity.power.graph = null;
            }
        }
        all.clear();
        producers.clear();
        consumers.clear();
        batteries.clear();
    }

    public void reflow(Tile tile){
        queue.clear();
        queue.addLast(tile);
        closedSet.clear();
        while(queue.size > 0){
            Tile child = queue.removeFirst();
            child.entity.power.graph = this;
            add(child);
            for(Tile next : child.block().getPowerConnections(child, outArray2)){
                if(next.entity.power != null && next.entity.power.graph == null && !closedSet.contains(next.pos())){
                    queue.addLast(next);
                    closedSet.add(next.pos());
                }
            }
        }
    }

    public void remove(Tile tile){
        clear();
        closedSet.clear();

        for(Tile other : tile.block().getPowerConnections(tile, outArray1)){
            if(other.entity.power == null || other.entity.power.graph != null){ continue; }
            PowerGraph graph = new PowerGraph();
            queue.clear();
            queue.addLast(other);
            while(queue.size > 0){
                Tile child = queue.removeFirst();
                child.entity.power.graph = graph;
                graph.add(child);
                for(Tile next : child.block().getPowerConnections(child, outArray2)){
                    if(next != tile && next.entity.power != null && next.entity.power.graph == null && !closedSet.contains(next.pos())){
                        queue.addLast(next);
                        closedSet.add(next.pos());
                    }
                }
            }
            // Update the graph once so direct consumers without any connected producer lose their power
            graph.update();
        }
    }

    private boolean otherConsumersAreValid(Tile tile, Consume consumePower){
        for(Consume cons : tile.block().consumes.all()){
            if(cons != consumePower && !cons.isOptional() && !cons.valid(tile.block(), tile.entity())){
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString(){
        return String.format(Locale.ROOT, "PowerGraph{producers=%s, consumers=%s, batteries=%s, all=%s, info=%s, lastFrameUpdated=%d, graphID=%d]",
                producers.toString(), consumers.toString(), batteries.toString(), all.toString(), info.toString(), lastFrameUpdated, graphID);
    }
}
