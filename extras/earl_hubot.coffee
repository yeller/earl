
# Description:
#   Allows Hubot to investigate ordasity via earl
#
# Dependencies:
#   None
#
# Configuration
#   HUBOT_EARL_URL the root url for earl
#   HUBOT_EARL_PASSWORD the http basic auth pass to use for making requests to earl
#
# Commands:
#   earl me cluster-name - summarize activity on a cluster
#
# Authors:
#   tcrayford

justify = (str, size) ->
  Array(size - str.length + 1).join(' ') + str

summarize_cluster_state = (cluster_state) ->
  total_load = 0
  for _, load of cluster_state["load-distribution"]
    total_load += load

  top6_nodes = ([node, load] for node, load of cluster_state["load-distribution"]).sort (a,b) -> b[0] - a[0]
  nodes_width = Math.max.apply null, (q[0].length for q in top6_nodes)
  formatted_nodes = ("#{justify(q[0], nodes_width + 1)} : #{q[1].toFixed(2)}" for q in top6_nodes).join '\n'

  top6_work_units = ([name, unit] for name, unit of cluster_state["work-units"]).sort (a,b) -> b[1]["load"] - a[1]["load"]
  work_units_width = Math.max.apply null, (q[0].length for q in top6_work_units)
  formatted_work_units = ("#{justify(q[0], work_units_width + 1)} : load=#{q[1]["load"].toFixed(2)} node=#{q[1]["node"]}" for q in top6_work_units).join '\n'

  "cluster state for #{cluster_state["cluster-name"]}\n
#{Object.keys(cluster_state["load-distribution"]).length} active nodes, #{Object.keys(cluster_state["work-units"]).length} work units, with a total load of #{total_load.toFixed(2)}\n\n

top 6 nodes\n" + formatted_nodes + "
\n\ntop 6 work units:\n" + formatted_work_units +
  "\n\nunclaimed work: #{cluster_state["unclaimed-work"].join('\n')}"

module.exports = (robot) ->
  robot.on 'error', (err) ->
    console.log(err)

  robot.respond /earl me (.+)$/i, (msg) ->
    unless process.env.HUBOT_EARL_URL
        msg.send "earl: need to set HUBOT_EARL_URL"
        return
    unless process.env.HUBOT_EARL_PASSWORD
        msg.send "earl: need to set HUBOT_EARL_PASSWORD"
        return
    auth = 'Basic ' + new Buffer('earl:' + process.env.HUBOT_EARL_PASSWORD).toString('base64')
    headers = { Accept: "application/json", 'Content-type': 'application/json' }
    headers['Authorization'] = auth
    msg.
      http("#{process.env.HUBOT_EARL_URL}/cluster/#{msg.match[1]}").
      headers(headers).
      get() (err, res, body) ->
        if err
          msg.send "earl: encountered an error #{err}"
          return
        unless res.statusCode is 200
          msg.send "earl: encountered a non 200 whilst asking earl for state, #{res.statusCode} #{body}"
          return
        msg.send(summarize_cluster_state(JSON.parse(body)))
