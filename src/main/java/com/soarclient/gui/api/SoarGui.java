package com.soarclient.gui.api;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import com.soarclient.Soar;
import com.soarclient.animation.Animation;
import com.soarclient.animation.Duration;
import com.soarclient.animation.cubicbezier.impl.EaseEmphasizedDecelerate;
import com.soarclient.gui.api.page.GuiTransition;
import com.soarclient.gui.api.page.SimplePage;
import com.soarclient.management.color.api.ColorPalette;
import com.soarclient.SoarConfig;
import com.soarclient.skia.Skia;
import com.soarclient.ui.component.Component;
import com.soarclient.utils.Multithreading;

import io.github.humbleui.skija.SurfaceOrigin;
import net.minecraft.client.gui.screen.Screen;

public abstract class SoarGui extends SimpleSoarGui {

	protected List<Component> components = new ArrayList<>();
	protected List<SimplePage> pages;

	protected SimplePage currentPage;
	protected SimplePage lastPage;

	private Animation inOutAnimation;
	private boolean closable;
	private Screen nextScreen;

	public SoarGui(boolean mcScale) {
		super(mcScale);

		this.pages = createPages();

		if (!pages.isEmpty()) {
			this.currentPage = pages.getFirst();
		}
	}

	@Override
	public void onResize() {
		if (currentPage != null) {
			setPageSize(currentPage);
			currentPage.init();
		}
	}

	@Override
	public void init() {
		setPageSize(currentPage);
		inOutAnimation = new EaseEmphasizedDecelerate(Duration.EXTRA_LONG_1, 0, 1);
		closable = true;
		currentPage.init();
	}

	public void setPageSize(SimplePage p) {
		p.setX(getX());
		p.setY(getY());
		p.setWidth(getWidth());
		p.setHeight(getHeight());
	}

	@Override
	public void draw(double mouseX, double mouseY) {

		ColorPalette palette = Soar.getInstance().getColorManager().getPalette();

		Skia.save();
		Skia.setAlpha((int) (inOutAnimation.getValue() * 255));
		Skia.scale(getX(), getY(), getWidth(), getHeight(), 2 - inOutAnimation.getValue());

		Skia.clip(getX(), getY(), getWidth(), getHeight(), 35);
		Skia.drawRoundedRect(getX(), getY(), getWidth(), getHeight(), 35, palette.getSurfaceContainer());

		if (currentPage != null && lastPage == null) {
			currentPage.draw(mouseX, mouseY);
		}

		if (lastPage != null) {

			GuiTransition transition = lastPage.getTransition();

			if (currentPage.getTransition().isConsecutive() && transition != null) {

				Skia.save();
				float[] result = transition.onTransition(lastPage.getAnimation());
				Skia.translate(result[0] * getWidth(), result[1] * getHeight());

				lastPage.draw(mouseX, mouseY);
				Skia.restore();
			}

			Skia.save();
			transition = currentPage.getTransition();

			if (transition != null) {
				float[] result = transition.onTransition(currentPage.getAnimation());
				Skia.translate(result[0] * getWidth(), result[1] * getHeight());
			}

			currentPage.draw(mouseX, mouseY);
			Skia.restore();

			if (lastPage.getAnimation().isFinished()) {
				lastPage = null;
			}
		}

		for (Component c : components) {
			c.draw(mouseX, mouseY);
		}

		// 必须 restore 两次：
		//   第一次弹出 setAlpha 内部的 saveLayer（把整面板按 alpha 合成），
		//   第二次弹出方法开头那次 save()。
		// 少调一次会让 saveLayer 与 clip 永久留在 Skia canvas 栈上，
		// 而 canvas 跨帧复用 —— 残留状态逐帧累积，导致后续绘制（含文字）越来越偏。
		Skia.restore();
		Skia.restore();

		if (inOutAnimation.getEnd() == 0 && inOutAnimation.isFinished()) {
			client.setScreen(nextScreen);
			nextScreen = null;
		}
	}

	@Override
	public void mousePressed(double mouseX, double mouseY, int button) {

		if (currentPage != null) {
			currentPage.mousePressed(mouseX, mouseY, button);
		}

		for (Component c : components) {
			c.mousePressed(mouseX, mouseY, button);
		}
	}

	@Override
	public void mouseReleased(double mouseX, double mouseY, int button) {

		if (currentPage != null) {
			currentPage.mouseReleased(mouseX, mouseY, button);
		}

		for (Component c : components) {
			c.mouseReleased(mouseX, mouseY, button);
		}
	}

	@Override
	public void mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {

		if (currentPage != null) {
			currentPage.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		}

		for (Component c : components) {
			c.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
		}
	}

	@Override
	public void mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (currentPage != null) {
			currentPage.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
		}
	}

	@Override
	public void charTyped(char chr, int modifiers) {

		if (currentPage != null) {
			currentPage.charTyped(chr, modifiers);
		}

		for (Component c : components) {
			c.charTyped(chr, modifiers);
		}
	}

	@Override
	public void keyPressed(int keyCode, int scanCode, int modifiers) {

		if (keyCode == GLFW.GLFW_KEY_ESCAPE && inOutAnimation.getEnd() == 1 && closable) {
			close();
		}

		if (currentPage != null) {
			currentPage.keyPressed(keyCode, scanCode, modifiers);
		}

		for (Component c : components) {
			c.keyPressed(keyCode, scanCode, modifiers);
		}
	}

	public void close(Screen nextScreen) {
		if (inOutAnimation.getEnd() == 1) {
			this.nextScreen = nextScreen;
			inOutAnimation = new EaseEmphasizedDecelerate(Duration.EXTRA_LONG_1, 1, 0);
			Multithreading.runAsync(() -> meteordevelopment.meteorclient.systems.Systems.save());
		}
	}

	public void close() {
		close(null);
	}

	public SimplePage getCurrentPage() {
		return currentPage;
	}

	public void setCurrentPage(SimplePage page) {

		if (currentPage != null) {
			lastPage = currentPage;
			currentPage.onClosed();
		}

		this.currentPage = page;
		currentPage.setAnimation(new EaseEmphasizedDecelerate(Duration.MEDIUM_1, 0, 1));
		if (lastPage != null) {
			lastPage.setAnimation(new EaseEmphasizedDecelerate(Duration.MEDIUM_1, 1, 0));
		}

		if (currentPage != null) {
			setPageSize(currentPage);
			currentPage.init();
		}
	}

	public void setCurrentPage(Class<? extends SimplePage> clazz) {

		SimplePage page = getPage(clazz);

		if (page != null) {
			setCurrentPage(page);
		}
	}

	public SimplePage getPage(Class<? extends SimplePage> clazz) {

		SimplePage page = null;

		for (SimplePage p : pages) {
			if (p.getClass().equals(clazz)) {
				page = p;
				break;
			}
		}

		return page;
	}

	public List<SimplePage> getPages() {
		return pages;
	}

	public boolean isClosable() {
		return closable;
	}

	public void setClosable(boolean closable) {
		this.closable = closable;
	}

	public abstract List<SimplePage> createPages();

	public abstract float getX();

	public abstract float getY();

	public abstract float getWidth();

	public abstract float getHeight();
}
