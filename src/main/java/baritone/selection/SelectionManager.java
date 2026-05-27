package baritone.selection;

import baritone.Baritone;
import baritone.api.selection.ISelection;
import baritone.api.selection.ISelectionManager;
import baritone.api.utils.BetterBlockPos;
import baritone.cache.WorldData;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.ListIterator;
import net.minecraft.core.Direction;

public class SelectionManager implements ISelectionManager {

    private static final long SELECTION_MAGIC = 0x73656C65637473L;

    private final LinkedList<ISelection> selections = new LinkedList<>();
    private ISelection[] selectionsArr = new ISelection[0];
    private final Baritone baritone;

    public SelectionManager(Baritone baritone) {
        this.baritone = baritone;
        new SelectionRenderer(baritone, this);
    }

    private void resetSelectionsArr() {
        selectionsArr = selections.toArray(new ISelection[0]);
    }

    @Override
    public synchronized ISelection addSelection(ISelection selection) {
        selections.add(selection);
        resetSelectionsArr();
        save();
        return selection;
    }

    @Override
    public ISelection addSelection(BetterBlockPos pos1, BetterBlockPos pos2) {
        return addSelection(new Selection(pos1, pos2));
    }

    @Override
    public synchronized ISelection removeSelection(ISelection selection) {
        selections.remove(selection);
        resetSelectionsArr();
        save();
        return selection;
    }

    @Override
    public synchronized ISelection[] removeAllSelections() {
        ISelection[] removed = getSelections();
        selections.clear();
        resetSelectionsArr();
        save();
        return removed;
    }

    @Override
    public ISelection[] getSelections() {
        return selectionsArr;
    }

    @Override
    public synchronized ISelection getOnlySelection() {
        if (selections.size() == 1) {
            return selections.peekFirst();
        }

        return null;
    }

    @Override
    public ISelection getLastSelection() {
        return selections.peekLast();
    }

    @Override
    public synchronized ISelection expand(ISelection selection, Direction direction, int blocks) {
        for (ListIterator<ISelection> it = selections.listIterator(); it.hasNext(); ) {
            ISelection current = it.next();

            if (current == selection) {
                it.remove();
                it.add(current.expand(direction, blocks));
                resetSelectionsArr();
                save();
                return it.previous();
            }
        }

        return null;
    }

    @Override
    public synchronized ISelection contract(ISelection selection, Direction direction, int blocks) {
        for (ListIterator<ISelection> it = selections.listIterator(); it.hasNext(); ) {
            ISelection current = it.next();

            if (current == selection) {
                it.remove();
                it.add(current.contract(direction, blocks));
                resetSelectionsArr();
                save();
                return it.previous();
            }
        }

        return null;
    }

    @Override
    public synchronized ISelection shift(ISelection selection, Direction direction, int blocks) {
        for (ListIterator<ISelection> it = selections.listIterator(); it.hasNext(); ) {
            ISelection current = it.next();

            if (current == selection) {
                it.remove();
                it.add(current.shift(direction, blocks));
                resetSelectionsArr();
                save();
                return it.previous();
            }
        }

        return null;
    }

    private synchronized void save() {
        WorldData world = baritone.getWorldProvider().getCurrentWorld();
        if (world == null) return;
        Path file = world.directory.resolve("selections.dat");
        try (DataOutputStream out = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(file)))) {
            out.writeLong(SELECTION_MAGIC);
            out.writeInt(selections.size());
            for (ISelection sel : selections) {
                out.writeInt(sel.pos1().x);
                out.writeInt(sel.pos1().y);
                out.writeInt(sel.pos1().z);
                out.writeInt(sel.pos2().x);
                out.writeInt(sel.pos2().y);
                out.writeInt(sel.pos2().z);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public synchronized void load(Path dir) {
        selections.clear();
        Path file = dir.resolve("selections.dat");
        if (Files.exists(file)) {
            try (DataInputStream in = new DataInputStream(new BufferedInputStream(Files.newInputStream(file)))) {
                if (in.readLong() == SELECTION_MAGIC) {
                    int count = in.readInt();
                    for (int i = 0; i < count; i++) {
                        BetterBlockPos pos1 = new BetterBlockPos(in.readInt(), in.readInt(), in.readInt());
                        BetterBlockPos pos2 = new BetterBlockPos(in.readInt(), in.readInt(), in.readInt());
                        selections.add(new Selection(pos1, pos2));
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        resetSelectionsArr();
    }
}
