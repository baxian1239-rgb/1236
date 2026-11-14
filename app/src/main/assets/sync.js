(() => {
  const prefix = 'mindos.cloud.';
  const read = (key, fallback) => JSON.parse(localStorage.getItem(prefix + key) || JSON.stringify(fallback));
  const write = (key, value) => localStorage.setItem(prefix + key, JSON.stringify(value));
  let session = read('session', null);
  const cacheKey = () => 'account.' + session.account;
  const entries = data => {
    const result = {};
    const put = (path, value) => { result[JSON.stringify(path)] = value; };
    for (const group of ['profile','m','d','e']) {
      for (const [key,value] of Object.entries(data[group] || {})) put([group,key],value);
    }
    for (const [month,slots] of Object.entries(data.xp || {})) {
      if (Array.isArray(slots)) slots.forEach((value,index) => { if (value) put(['xp',month,index],true); });
    }
    for (const quest of data.quests || []) put(['quests',quest.id],quest);
    if (data.canvas) put(['canvas'],data.canvas);
    return result;
  };
  const decode = fields => {
    const data = {profile:{},m:{},d:{},e:{},xp:{},quests:[],canvas:''};
    for (const [key, entry] of Object.entries(fields)) {
      if (entry.deleted) continue;
      const [group,name,index] = JSON.parse(key);
      if (['profile','m','d','e'].includes(group)) Object.defineProperty(data[group],name,{value:entry.value,enumerable:true,writable:true,configurable:true});
      if (group === 'canvas') data.canvas = entry.value;
      if (group === 'quests') data.quests.push(entry.value);
      if (group === 'xp' && /^\d{4}-\d{2}$/.test(name) && Number.isInteger(index) && index >= 0 && index < 100) {
        (data.xp[name] ||= Array(100).fill(false))[index] = !!entry.value;
      }
    }
    return data;
  };
  const equal = (a,b) => JSON.stringify(a) === JSON.stringify(b);
  const value = entry => entry?.deleted ? undefined : entry?.value;
  const load = legacy => session ? decode(read(cacheKey(),{fields:{}}).fields) : legacy;
  function save(data) {
    const cache = read(cacheKey(),{base:{},fields:{}});
    const next = entries(data);
    for (const key of new Set([...Object.keys(cache.fields),...Object.keys(next)])) {
      if (!equal(value(cache.fields[key]),next[key])) {
        cache.fields[key] = next[key] === undefined ? {deleted:true,at:Date.now()} : {value:next[key],at:Date.now()};
      }
    }
    write(cacheKey(),cache);
  }
  async function api(action, body, auth = session) {
    let result;
    if (window.AndroidApp) {
      result = JSON.parse(await new Promise((resolve,reject) => {
        const id = String(++serial);
        const timer = setTimeout(() => { callbacks.delete(id); reject(Error('连接超时')); },25000);
        callbacks.set(id, text => { clearTimeout(timer); resolve(text); });
        AndroidApp.cloudRequest(id,action,JSON.stringify(body),auth?.token || '');
      }));
    } else {
      const res = await fetch('api.php?action='+action,{method:'POST',headers:{'Content-Type':'application/json','X-MindOS-Token':auth?.token || ''},
        body:JSON.stringify(body),signal:AbortSignal.timeout(25000)});
      result = {status:res.status,body:await res.json()};
    }
    if (result.status >= 400 || !result.status) throw Error(result.body.error || '同步失败');
    return result.body;
  }
  const callbacks = new Map();
  let serial = 0;
  window.mindCloudResponse = (id,response) => {
    const callback = callbacks.get(id);
    callbacks.delete(id);
    callback?.(response);
  };
  let busy = false;
  let status;
  async function sync() {
    if (!session || busy || document.hidden || document.activeElement?.matches('textarea,input') || document.querySelector('#drawCanvas:active')) return;
    busy = true;
    const auth = session;
    status.textContent = '正在同步';
    try {
      const remote = await api('sync',{},auth);
      if (session !== auth) return;
      const cache = read(cacheKey(),{base:{},fields:{}});
      const merged = {...remote.fields};
      for (const [key,local] of Object.entries(cache.fields)) {
        if (equal(local,cache.base[key])) continue;
        const online = remote.fields[key];
        if (equal(value(local),value(online))) continue;
        if (online && !equal(online,cache.base[key])) {
          if (document.activeElement?.matches('textarea,input') || document.querySelector('#drawCanvas:active')) return;
          const format = item => {
            const entry = value(item);
            if (entry === undefined) return '（已删除）';
            if (typeof entry === 'string' && entry.startsWith('data:image/')) return '画板图片';
            return JSON.stringify(entry).slice(0,350);
          };
          const keep = confirm('两端修改了同一项 '+key+'\n本机 '+format(local)+'\n云端 '+format(online)+'\n确定保留本机，取消保留云端');
          if (!keep) continue;
        }
        merged[key] = local;
      }
      const saved = equal(merged,remote.fields) ? remote : await api('sync',{revision:remote.revision,fields:merged},auth);
      if (session !== auth) return;
      const current = read(cacheKey(),{base:{},fields:{}});
      const next = {...saved.fields};
      for (const [key,entry] of Object.entries(current.fields)) {
        if (!equal(entry,cache.fields[key])) next[key] = entry;
      }
      const changed = !equal(decode(next),decode(current.fields));
      if (changed && (document.activeElement?.matches('textarea,input') || document.querySelector('#drawCanvas:active'))) {
        status.textContent = '编辑中，稍后接收云端更新';
        return;
      }
      write(cacheKey(),{...current,base:saved.fields,fields:next});
      status.textContent = equal(next,saved.fields) ? '已同步 '+new Date().toLocaleTimeString() : '有本机修改待同步';
      if (changed) location.reload();
    } catch (error) {
      status.textContent = '同步暂停，修改保存在本机 · '+error.message;
    } finally { busy = false; }
  }
  function setup(getLegacy) {
    const box = document.querySelector('#cloudAccount');
    status = box.querySelector('output');
    const summary = document.querySelector('#syncSummary');
    if (summary) {
      const update = () => {
        const message = status.textContent;
        summary.textContent = message.startsWith('同步暂停') ? '同步失败' : message;
        summary.title = message;
      };
      new MutationObserver(update).observe(status, {childList:true,subtree:true,characterData:true});
      update();
    }
    const form = box.querySelector('form');
    const logout = box.querySelector('[data-logout]');
    form.hidden = !!session;
    logout.hidden = !session;
    status.textContent = session ? '已登录 '+session.account+' · 等待同步' : '使用本地数据';
    form.onsubmit = async event => {
      event.preventDefault();
      if (busy) return;
      busy = true;
      try {
        const auth = await api(event.submitter.value,{name:form.elements.account.value,password:form.elements.password.value},null);
        const key = 'account.'+auth.account;
        const cache = read(key,{base:{},fields:{},asked:false});
        if (!cache.asked && !read('legacyOwner',null) && Object.keys(entries(getLegacy())).length &&
            confirm('是否把此设备的现有本地记录合并到 '+auth.account+'？原始本地数据会保留。')) {
          for (const [field,value] of Object.entries(entries(getLegacy()))) {
            cache.fields[field] = {value,at:Date.now()};
          }
          write('legacyOwner',auth.account);
        }
        cache.asked = true;
        write(key,cache);
        write('session',auth);
        location.reload();
      } catch (error) { status.textContent = error.message; }
      finally { form.elements.password.value = ''; busy = false; }
    };
    logout.onclick = async () => {
      if (busy) return;
      busy = true;
      try {
        await api('logout',{});
      } catch {
        if (!confirm('无法在线注销服务器会话。退出本机后，待同步数据会保留在该账号的本机缓存中。继续？')) return;
      } finally { busy = false; }
      localStorage.removeItem(prefix+'session');
      location.reload();
    };
    setInterval(sync,5000);
    addEventListener('online',sync);
    document.addEventListener('visibilitychange',sync);
    sync();
  }
  window.MindCloud = {load,save,setup,get active(){return !!session;}};
})();





