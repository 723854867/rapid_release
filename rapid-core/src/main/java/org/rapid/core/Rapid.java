package org.rapid.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.rapid.core.bean.enums.Env;
import org.rapid.core.bean.enums.Locale;
import org.rapid.core.bean.exception.BizException;
import org.rapid.core.initial.InitialHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStartedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.stereotype.Component;

@Component
public class Rapid implements ApplicationListener<ApplicationContextEvent> {
	
	private static final Logger logger = LoggerFactory.getLogger(Rapid.class);

	private Env env;
	private Locale locale;
	private boolean initial;
	
	@Override
	public void onApplicationEvent(ApplicationContextEvent event) {
		if (event instanceof ContextClosedEvent) {
			if (initial)
				_dispose();
		} else if (event instanceof ContextRefreshedEvent) {
			logger.info("spring container refreshed!");
			if (!initial)
				_initial();
		} else if (event instanceof ContextStartedEvent) {
			logger.info("spring container started!");
			if (!initial)
				_initial();
		} else if (event instanceof ContextStoppedEvent) {
			if (initial)
				_dispose();
		} else 
			logger.error("Receive unrecognizable spring event {}!", event);
	}
	
	private synchronized void _initial() {
		if (initial)
			return;
		this.initial = true;
		logger.info("spring container initialize success, start initialize application...");
		long start = System.nanoTime();
		_configurationParser();
		Map<String, InitialHook> hooks = SpringContextUtil.getApplicationContext().getBeansOfType(InitialHook.class, false, true);
		List<InitialHook> list = new ArrayList<InitialHook>(hooks.values());
		Collections.sort(list, (o1, o2) -> o1.priority() - o2.priority());
		for (InitialHook hook : list)
			try {
				hook.init();
			} catch (Exception e) {
				throw new BizException(e);
			}
		long end = System.nanoTime();
		logger.info("application initial success in {} seconds!", TimeUnit.NANOSECONDS.toSeconds(end - start));
	}
	
	private void _dispose() {
		if (!initial)
			return;
		this.initial = false;
		logger.info("start stop application...");
		long start = System.nanoTime();

		long end = System.nanoTime();
		logger.info("application stop success in {} seconds!", TimeUnit.NANOSECONDS.toSeconds(end - start));
	}
	
	private void _configurationParser() {
		this.env = RapidConfiguration.get(CoreConsts.RAPID_ENV, true);
		this.locale = RapidConfiguration.get(CoreConsts.RAPID_LOCALE, true);
	}
	
	public Env getEnv() {
		return env;
	}
	
	public Locale getLocale() {
		return locale;
	}
}
